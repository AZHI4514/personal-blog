# Personal Blog

Vue 3 + Spring Boot personal blog, prepared for local development and production deployment.

## Stack

- Frontend: Vue 3, Vite, Vue Router, Pinia, Axios
- Backend: Spring Boot 3.5, MyBatis, MySQL

## Project Layout

```text
personal-blog/
├─ frontend/
└─ backend/
```

## Live2D SDK 部署与动作设置

这个项目将 Live2D 直接集成在 `frontend/src/App.vue` 中，没有额外创建单独的 Vue 组件。

### 1. 本项目使用的目录结构

将原始 SDK 和原始模型文件保留在仓库根目录：

```text
personal-blog/
├─ Live2d/
│  ├─ CubismSdkForWeb-5-r.5/
│  └─ Yachiyo/
├─ frontend/
│  ├─ public/
│  │  ├─ Core/
│  │  ├─ Framework/Shaders/
│  │  └─ Resources/Yachiyo/
│  └─ src/App.vue
```

前端运行时实际使用的文件：

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/Yachiyo/*`

构建时使用的源码文件：

- `Live2d/CubismSdkForWeb-5-r.5/Framework/src/*`
- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src/*`

### 2. 将 SDK 运行时文件复制到 `frontend/public`

浏览器在运行时不能直接从仓库根目录加载文件，所以必须把运行时资源复制到 `frontend/public`。

必须复制的内容：

```text
Live2d/CubismSdkForWeb-5-r.5/Core
  -> frontend/public/Core

Live2d/CubismSdkForWeb-5-r.5/Framework/Shaders
  -> frontend/public/Framework/Shaders

Live2d/Yachiyo
  -> frontend/public/Resources/Yachiyo
```

本项目还使用了这两个 sample 背景资源：

```text
Live2d/CubismSdkForWeb-5-r.5/Samples/Resources/back_class_normal.png
  -> frontend/public/Resources/back_class_normal.png

Live2d/CubismSdkForWeb-5-r.5/Samples/Resources/icon_gear.png
  -> frontend/public/Resources/icon_gear.png
```

### 3. 为官方 SDK 源码配置 Vite 别名

`frontend/vite.config.js` 需要两个别名：

- `@framework`
  指向 `../Live2d/CubismSdkForWeb-5-r.5/Framework/src`
- `@live2d-demo`
  指向 `../Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src`

这个项目直接从这些路径导入官方 sample 类，而不是把 sample 源码复制到 `frontend/src` 里。

同时还启用了：

- `server.fs.allow: ['..']`

这是必须的，因为别名目标目录位于 `frontend/` 外部。

### 4. `App.vue` 是如何挂载 Live2D 模型的

当前接入逻辑全部写在 `frontend/src/App.vue` 中。

主要部分如下：

- 从 Vue 中增加 `watch` 和 `nextTick` 导入。
- 增加 `live2dCanvas`、`live2dError`，以及若干 SDK 运行时变量。
- 增加 `ensureLive2dCoreLoaded()`，用于只向页面注入一次 `/Core/live2dcubismcore.js`。
- 增加 `loadLive2dSdk()`，用于动态导入：
  - `@framework/live2dcubismframework`
  - `@live2d-demo/lapppal`
  - `@live2d-demo/lappdefine`
  - `@live2d-demo/lappsubdelegate`
  - `@live2d-demo/lappview`
- Patch `LAppView.prototype.initializeSprite()`，让 sample 渲染器不再依赖原 sample 的背景初始化逻辑。
- Patch `LAppSubdelegate.prototype.update()`，让 `gl.clearColor(0, 0, 0, 0)` 把画布背景清成透明。
- 强制将 `live2dDefine.ModelDir` 设为 `['Yachiyo']`。
- 只在 `currentPage === 'games'` 时挂载模型。
- 在离开游戏页或卸载应用时销毁渲染器和动画帧。

模板侧只需要一个 canvas 挂载点：

```html
<div v-if="currentPage === 'games'" class="game-container">
  <div class="live2d-stage">
    <canvas ref="live2dCanvas" class="live2d-canvas"></canvas>
    <div v-if="live2dError" class="live2d-error">{{ live2dError }}</div>
  </div>
</div>
```

一个很重要的页面名细节：

- 菜单点击使用的是 `showPage('games')`
- 页面区块判断也必须使用 `currentPage === 'games'`

如果一边写成 `game`，另一边写成 `games`，模型就永远不会挂载。

### 5. 为什么必须修正模型配置文件

运行时使用的模型配置文件是：

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`

在这个项目里，模型一开始只显示黑屏，是因为 `Yachiyo.model3.json` 里的文件名是乱码，和磁盘上的真实文件名不一致。

这里采用的稳定修复方式，是把文件名写成 Unicode 转义形式，例如：

```json
{
  "Moc": "\u516b\u5343\u4ee3\u8f89\u591c\u59ec.moc3"
}
```

这样既能避免路径因为编码问题损坏，又能正确解析到真实的中文文件名。

如果以后模型再次变成空白画布或黑画布，优先先检查这个文件。

### 6. 当前表情行为

当前项目中的运行时行为如下：

- 鼠标在 Live2D 画布上移动时，会更新角色的面部和身体跟随行为
- 鼠标离开画布时，会把跟随目标重置回中心
- 点击画布时，会触发一次随机面部表情

当前 Yachiyo 模型的状态：

- 已配置表情
- 本项目中已刻意禁用身体动作播放

这意味着角色现在只保留“指针跟随”和“表情切换”两类行为。

### 7. 如何配置表情

表情定义写在 `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` 中：

```json
"Expressions": [
  { "Name": "tear", "File": "\u773c\u6cea.exp3.json" },
  { "Name": "smile_eye", "File": "\u772f\u772f\u773c.exp3.json" },
  { "Name": "smile", "File": "\u7b11\u54aa\u54aa.exp3.json" },
  { "Name": "blink", "File": "\u6cea\u73e0.exp3.json" }
]
```

如果要新增一个表情：

1. 把新的 `.exp3.json` 文件放进 `frontend/public/Resources/Yachiyo/`。
2. 在 `Yachiyo.model3.json` 的 `"Expressions"` 下增加一条新记录。
3. 文件名必须完全一致；如果文件名包含中文，推荐在 JSON 里使用 Unicode 转义形式。

由于 sample 代码已经调用了 `setRandomExpression()`，所以这里新增的表情会自动加入到随机点击切换中。

### 8. 鼠标跟随是如何实现的

鼠标跟随逻辑写在 `frontend/src/App.vue` 中。

本项目的实现步骤如下：

1. 保留官方 `LAppSubdelegate` 渲染器。
2. 把 `pointermove` 绑定到 Live2D canvas，而不是只在拖拽时才追踪。
3. 将 canvas 内部局部坐标换算成 Live2D 视图坐标。
4. 在每次移动事件中调用 `subdelegate.getLive2DManager().onDrag(viewX, viewY)`。
5. 在 `pointerleave` 时调用 `onDrag(0.0, 0.0)`，让模型回到中性位置。

一个重要细节：

- 原始 sample 只会在指针被捕获时更新拖拽状态
- 这个项目重写了这部分行为，所以跟随是在悬停时生效，而不只是拖拽时生效

### 9. 点击触发随机表情是如何实现的

点击行为是基于官方 sample manager 进行 patch 的，原始位置在：

- `Live2d/CubismSdkForWeb-5-r.5/Samples/TypeScript/Demo/src/lapplive2dmanager.ts`

这个项目重写了 `LAppLive2DManager.prototype.onTap()`，让它只做一件事：

- 调用 `model.setRandomExpression()`

这也同时移除了 sample 里原本“点击身体触发动作”的行为。

实现步骤如下：

1. 在 `App.vue` 中导入 `@live2d-demo/lapplive2dmanager`。
2. Patch `LAppLive2DManager.prototype.onTap`。
3. 在 patch 后的函数中取出 `this._models[0]`。
4. 如果模型存在，则调用 `setRandomExpression()`。
5. 不再调用 `startRandomMotion(...)`。

### 10. 如何新增或修改点击表情

当前可用于随机点击切换的表情仍然来自：

- `frontend/public/Resources/Yachiyo/Yachiyo.model3.json`

如果你在 `"Expressions"` 下继续新增表情条目，随机点击行为会自动把它们包含进去。

如果你不想随机，而是想固定切换到某个表情，可以把：

```js
model.setRandomExpression()
```

替换成：

```js
model.setExpression('smile')
```

### 11. 透明背景与页面外框

这个项目里的透明画布效果，不是只改 CSS 就够了。

必须同时满足两件事：

1. CSS:
   - `.live2d-canvas { background: transparent; }`
2. WebGL clear alpha:
   - `gl.clearColor(0.0, 0.0, 0.0, 0.0)`

如果只有 CSS 是透明的，但 WebGL 仍然用 alpha `1` 清屏，那么模型区域看起来还是黑的。

外围框体样式同样写在 `App.vue` 中，用来和博客的 BBS / post-form 视觉风格保持一致：

- 外层包裹：`.game-container`
- 内层面板：`.live2d-stage`

### 12. 排错清单

如果模型没有显示：

1. 确认页面判断条件是 `currentPage === 'games'`。
2. 确认 `frontend/public/Core/live2dcubismcore.js` 存在。
3. 确认 `frontend/public/Framework/Shaders/WebGL/*` 存在。
4. 确认 `frontend/public/Resources/Yachiyo/Yachiyo.model3.json` 存在。
5. 确认 `Yachiyo.model3.json` 里的文件名与真实文件名完全一致。
6. 确认浏览器网络面板能够成功加载：
   - `/Core/live2dcubismcore.js`
   - `/Resources/Yachiyo/Yachiyo.model3.json`
   - 以及它引用的 `.moc3`、`.physics3.json`、`.exp3.json`、贴图文件
7. 如果你只看到黑色矩形，优先检查模型 JSON 里的路径。
8. 如果鼠标跟随失效，检查 canvas 的 `pointermove` 处理器是否还在调用 `onDrag(viewX, viewY)`。
9. 如果点击后脸部表情没有变化，检查 `"Expressions"` 区段是否存在，以及 `onTap()` 是否仍然被 patch 为 `setRandomExpression()`。

### 13. 替换模型时推荐的流程

当你要把 `Yachiyo` 替换成其他模型时：

1. 把新模型文件夹复制到 `frontend/public/Resources/<ModelName>/`。
2. 确保 `<ModelName>.model3.json` 文件存在。
3. 修正 JSON 中所有乱码文件名。
4. 更新 `App.vue` 中对 `ModelDir` 的覆盖：

```js
live2dDefine.ModelDir.splice(0, live2dDefine.ModelDir.length, '<ModelName>')
```

5. 重新构建：

```bash
cd frontend
npm run build
```

### 14. Live2D 显示与加载问题的解决记录

这次实际遇到的问题不是“第一次加载失败”，而是：

1. 第一次进入“游戏角”时，Live2D 可以正常显示。
2. 切到其他页面后，再切回“游戏角”时，Live2D 会出现下面几种异常之一：
   - 直接不显示
   - 还能看到，但只剩最后一帧，变成静态
   - 重新初始化后偶尔再次消失

这类问题的本质是：

- 这是一个单页应用，页面切换并不是浏览器整页刷新。
- Live2D 依赖 `canvas + WebGL + CubismFramework + sample runtime` 这一整套运行时状态。
- 如果页面切换时只销毁一半状态，或者重新进入页面时初始化时机早于 `canvas` 真正可用，就会出现“第二次不稳定”。

这次最终确认下来的关键经验有这几条：

#### 1. 游戏角容器不要再用 `v-if` 反复卸载

如果游戏角外层用：

```html
<div v-if="currentPage === 'games'" class="game-container">
```

那么切出页面时，整个 `canvas` 会从 DOM 里被删掉。  
Live2D sample 在这种“反复销毁 DOM 再重建 WebGL”的路径上不稳定，很容易造成第二次加载异常。

这次改成：

```html
<div v-show="currentPage === 'games'" class="game-container">
```

这样页面切换只是隐藏和显示，不会把 Live2D 的宿主 DOM 直接卸载掉。

#### 2. 回到游戏角时，不能假设 `canvas` 已经马上可用

单页应用切页时，`currentPage` 改了，不代表 `canvas ref` 这一刻已经可用。  
所以回到游戏角时要这样做：

```js
watch(currentPage, async (pageName) => {
  if (pageName === 'games') {
    await nextTick()
    await mountLive2d()
  }
})
```

`nextTick()` 的作用是等 Vue 先把显示状态更新到 DOM，再去初始化 Live2D。

#### 3. 只监听 `currentPage` 还不够，最好再监听一次 `live2dCanvas`

有些情况下：
- `currentPage` 已经切回 `games`
- 但 `canvas ref` 仍然还没稳定

所以又补了一层：

```js
watch(live2dCanvas, async (canvas) => {
  if (!canvas || currentPage.value !== 'games') return
  await mountLive2d()
})
```

这层逻辑的目的不是替代 `currentPage` watcher，而是做一次兜底补挂载。

#### 4. “显示正常” 和 “交互正常” 是两件事

这次排查里还发现：

- Live2D 能显示，不代表渲染循环还在跑
- 画面还在，不代表模型还会动
- 交互事件绑在 `canvas` 上，不代表页面切回来后这些事件还一定有效

所以需要把下面几部分明确拆开：

- `mountLive2d()`：负责初始化 SDK、创建 subdelegate、绑定交互、启动渲染
- `destroyLive2d()`：负责停止动画帧、解绑事件、释放 subdelegate、清理 CubismFramework
- `attachLive2dPointerEvents()`：只负责交互绑定
- `detachLive2dPointerEvents()`：只负责交互解绑

不要把这些逻辑糊在一起，不然后面很难判断到底是：
- 没有显示
- 没有重新渲染
- 还是只是鼠标跟随失效

#### 5. 鼠标跟随范围已经从画布扩展到了整个页面

之前的逻辑是：
- 鼠标只要离开 `canvas`，角色就会停止跟随

这次改成了：
- `pointerdown` 仍然绑定在 `canvas` 上，用来处理点击交互
- `pointermove / pointerup / pointercancel` 绑定到 `window`
- 再把整个页面视口坐标映射到 Live2D 视图坐标

这样鼠标在整个页面移动时，角色都可以继续跟随。

#### 6. 这次修复过程中最重要的排错结论

如果以后再次出现“第一次正常、第二次异常”的问题，优先按下面顺序检查：

1. 游戏角容器是不是又被改回 `v-if`
2. 切回页面时是不是少了 `await nextTick()`
3. `live2dCanvas` 的 watcher 是不是被删掉了
4. `destroyLive2d()` 是否真的完整停止了动画帧并清理了框架
5. `mountLive2d()` 是否真的被再次调用
6. 第二次进入页面时，`canvas ref` 是否存在
7. 第二次进入页面时，`subdelegate.initialize(canvas)` 是否返回成功

### 15. 从零实现整个 Live2D 加载流程

如果以后你想完全从零重新做一套 Live2D 加载，推荐按下面这个顺序来，不要一开始就把模型、交互、表情、页面切换逻辑一次性全部写进去。

#### 第一步：准备运行时资源

浏览器运行 Live2D 时，真正需要的是这些资源：

- `frontend/public/Core/live2dcubismcore.js`
- `frontend/public/Framework/Shaders/WebGL/*`
- `frontend/public/Resources/<ModelName>/*`

所以从零接入时，先把 SDK 运行时文件和模型资源复制到 `frontend/public` 下。

#### 第二步：准备 Vite 别名

在 `frontend/vite.config.js` 里给 SDK 源码和 sample 源码配置别名：

- `@framework`
- `@live2d-demo`

并允许 Vite 读取 `frontend/` 目录之外的 SDK 源码路径。

#### 第三步：准备最小运行时状态

至少需要下面这些状态：

- `live2dCanvas`
- `live2dError`
- `live2dSdk`
- `live2dSubdelegate`
- `live2dAnimationFrame`
- `live2dPointerHandlers`
- `live2dFrameworkReady`

这几个变量的职责要明确：

- `live2dCanvas`：拿到 canvas DOM
- `live2dError`：给页面显示错误
- `live2dSdk`：缓存 SDK 模块
- `live2dSubdelegate`：当前模型运行实例
- `live2dAnimationFrame`：渲染循环句柄
- `live2dPointerHandlers`：缓存事件处理器，方便解绑
- `live2dFrameworkReady`：避免重复 `startUp()/initialize()`

#### 第四步：实现 `ensureLive2dCoreLoaded()`

这个函数要做的事情只有一个：

- 确保 `/Core/live2dcubismcore.js` 只被注入一次

推荐逻辑：

1. 如果 `window.Live2DCubismCore` 已存在，直接返回
2. 如果页面里已有 `script[data-live2d-core="true"]`，等待它加载完成
3. 否则动态创建 `<script>` 注入到 `document.head`

#### 第五步：实现 `loadLive2dSdk()`

这个函数负责动态导入：

- `@framework/live2dcubismframework`
- `@live2d-demo/lapppal`
- `@live2d-demo/lappdefine`
- `@live2d-demo/lappsubdelegate`
- `@live2d-demo/lappview`
- `@live2d-demo/lapplive2dmanager`

然后在这里做必要的 patch，例如：

- 把背景清成透明
- 修改点击逻辑，只保留表情切换
- 指定固定模型目录

#### 第六步：实现 `mountLive2d()`

这个函数应该只负责“挂载”，不要顺手做销毁逻辑。

建议顺序：

1. 判断当前页面是否真的是 `games`
2. `await nextTick()`
3. 确认 `live2dCanvas.value` 存在
4. 调用 `loadLive2dSdk()`
5. 如果框架还没初始化，执行：
   - `CubismFramework.startUp(option)`
   - `CubismFramework.initialize()`
6. 创建新的 `LAppSubdelegate`
7. 调用 `subdelegate.initialize(live2dCanvas.value)`
8. 绑定指针事件
9. 启动 `requestAnimationFrame` 渲染循环

#### 第七步：实现 `destroyLive2d()`

这个函数应该只负责“完整销毁”：

1. 停止 `requestAnimationFrame`
2. 解绑所有指针事件
3. `subdelegate.release()`
4. `CubismFramework.dispose()`
5. `CubismFramework.cleanUp()`
6. 清掉本地缓存状态

不要把“暂停一部分”和“完整销毁”写成同一个模糊动作，否则页面切换时很容易进入半死不活的状态。

#### 第八步：最后再接页面切换

等单次挂载稳定后，再接页面切换：

```js
watch(currentPage, async (pageName) => {
  if (pageName === 'games') {
    await nextTick()
    await mountLive2d()
    return
  }

  destroyLive2d()
})
```

然后再补一层：

```js
watch(live2dCanvas, async (canvas) => {
  if (!canvas || currentPage.value !== 'games') return
  await mountLive2d()
})
```

#### 第九步：最后再接交互增强

等“稳定显示”和“稳定切页恢复”都没问题后，再去加这些增强项：

- 页面级鼠标跟随
- 点击切换表情
- 房间状态卡
- 音乐联动
- Agent 对话面板

顺序一定不要反过来。  
先把“能稳定显示、能稳定第二次进入”做好，再去做交互层。这样以后排错会容易很多。

## 用户认证逻辑（当前实现）

### 1. 本站当前是否使用 JWT

当前实现 **没有使用 JWT（JSON Web Token）作为用户认证方案**。


1. 后端登录接口 `POST /users/login` 成功后，没有签发 token，也没有返回 JWT 字符串。
2. 后端在 [backend/src/main/java/com/azhi/controller/UserController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/UserController.java:18) 中，登录和注册成功后做的是：
   - `session.setAttribute("currentUser", user)`
3. 退出登录时走的是：
   - `session.invalidate()`
4. 发帖权限校验在 [backend/src/main/java/com/azhi/controller/PostController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/PostController.java:29) 中依赖的是 `HttpSession`，而不是 Bearer ***。

所以本站当前的认证核心是：

- **服务端会话：Spring `HttpSession`**
- **前端展示态缓存：`localStorage.currentUser`**

而不是：

- JWT
- OAuth
- 无状态 Bearer *** 鉴权

### 2. 注册、登录、退出的真实流程

#### 注册

前端调用：

- `POST /users/register`

后端流程：

1. 校验用户名、密码、邮箱是否为空
2. 校验用户名和邮箱是否已存在
3. 使用 `BCryptPasswordEncoder` 对密码加密
4. 把用户写入数据库
5. 清掉返回对象中的密码字段
6. 把用户对象写入 `HttpSession`

对应代码：

- [backend/src/main/java/com/azhi/service/impl/UserServiceImpl.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/service/impl/UserServiceImpl.java:14)
- [backend/src/main/java/com/azhi/controller/UserController.java](/abs/path/D:/personal-blog/backend/src/main/java/com/azhi/controller/UserController.java:21)

#### 登录

前端调用：

- `POST /users/login`

后端流程：

1. 根据用户名查询数据库
2. 使用 `BCryptPasswordEncoder.matches()` 校验明文密码与数据库哈希密码是否匹配
3. 登录成功后，将用户对象写入 `HttpSession`
4. 返回不带密码的用户信息给前端

#### 退出

前端调用：

- `POST /users/logout`

后端流程：

1. 调用 `session.invalidate()`
2. 服务端会话失效
3. 前端再清掉本地 `currentUser`

### 3. 前端是如何保存登录状态的

前端在 [frontend/src/App.vue](/abs/path/D:/personal-blog/frontend/src/App.vue:19) 中使用：

```js
const currentUser = ref(JSON.parse(localStorage.getItem('currentUser') || 'null'))
```

登录成功后：

- 把后端返回的用户对象写入 `currentUser`
- 同时执行：
  - `localStorage.setItem('currentUser', JSON.stringify(user))`

退出登录后：

- `currentUser.value = null`
- `localStorage.removeItem('currentUser')`

要注意：

- 这个 `currentUser` 只是前端界面状态缓存，不是服务端鉴权凭证本身。
- 真正决定“这个请求是否已登录”的，仍然是服务端 `HttpSession`。

### 4. 发帖与管理权限是怎么判断的

#### 发帖/删除等需要登录的接口

例如帖子相关接口，在后端通过 `HttpSession` 判断：

- `session.getAttribute("currentUser") == null` 就视为未登录

这说明本站不是靠 JWT 解 token 判断身份，而是标准 Session 方案。

#### 管理员权限

当前管理员判断是比较直接的用户名判断：

- 用户名为 `AZHI4514` 时视为管理员

前端里对应：

- `const isAdmin = computed(() => currentUser.value?.username === 'AZHI4514')`

后端房间 Agent 配置接口里还使用了：

- `X-Admin-User`

但这只是当前项目里一个非常轻量的管理开关，不是完整的权限系统。

### 5. 关于 `frontend/src/api/request.js` 里的 token 逻辑

仓库里可以看到 [frontend/src/api/request.js](/abs/path/D:/personal-blog/frontend/src/api/request.js:13) 会尝试从 `localStorage` 读取 `token` 并附加：

```js
Authorization: Bearer <token>
```

但就当前代码路径来说：

1. 登录接口没有签发 JWT
2. 前端登录流程也没有保存 `token`
3. 后端用户认证逻辑没有校验 Bearer ***

所以这段 `token` 逻辑目前 **不是本站实际在用的认证主链路**，更像是以前或后续扩展预留。

结论可以明确写成一句话：

- **本站当前不是 JWT 认证，而是 Session 认证。**

### 6. 当前认证方案的特点

优点：

- 实现简单
- 对当前博客项目足够直接
- 后端权限判断容易落地

局限：

- 前后端分离到跨域部署时，要额外处理 Cookie / Session
- 不适合直接扩展成标准无状态 API 鉴权
- 管理员权限目前还是写死用户名，不是完整 RBAC

如果以后要改成 JWT，通常需要一起调整：

1. 登录接口签发 access token
2. 前端保存 token
3. 后端增加 JWT 校验过滤器
4. 发帖、删除、管理等接口改成从 token 中取用户身份

## Live2D 章节状态校正（以当前代码为准）

下面这几条是对前面 Live2D 章节的补充校正，避免旧描述和当前代码状态不一致。

### 1. 游戏角容器当前使用的是 `v-show`，不是 `v-if`

当前代码以 [frontend/src/App.vue](/abs/path/D:/personal-blog/frontend/src/App.vue:1991) 为准：

```html
<div v-show="currentPage === 'games'" class="game-container">
```

这意味着：

- 切出游戏角时，DOM 不会被真正卸载
- 只是隐藏显示状态切换

如果 README 里前面仍有 `v-if` 示例，请以现在这条为准。

### 2. 鼠标跟随范围当前已经扩大到整个页面

当前代码里：

- `pointerdown` 仍然绑在 `canvas`
- `pointermove / pointerup / pointercancel` 已经绑到 `window`

所以现在的实际行为不是“只有鼠标在画布上移动才跟随”，而是：

- **整个页面范围内移动鼠标，Live2D 视角都会跟随**

### 3. 页面切换时，当前代码仍然会做完整销毁再重建

当前 `watch(currentPage, ...)` 的行为是：

- 进入 `games`：
  - `await nextTick()`
  - `await mountLive2d()`
- 离开 `games`：
  - `destroyLive2d()`

也就是说，当前代码状态不是“后台持续保留旧实例不动”，而是“切走销毁，切回重建”。

### 4. 当前还保留了 `watch(live2dCanvas, ...)` 作为补挂载机制

这条逻辑仍然存在，作用是：

- 当 `canvas ref` 真正可用时，再补一次 `mountLive2d()`

所以 README 前文如果只写了 `watch(currentPage)`，还不够完整。  
当前真实代码是：

- `watch(currentPage, ...)`
- `watch(live2dCanvas, ...)`

两层一起工作。

### 5. 当前 Live2D 状态描述建议以这几条为准

如果你以后继续维护 README，建议把 Live2D 现状统一理解成：

1. 运行时资源来自 `frontend/public`
2. SDK 源码通过 `@framework` / `@live2d-demo` 动态导入
3. 游戏角区域通过 `v-show` 保留宿主 DOM
4. 进入页面时通过 `nextTick() + mountLive2d()` 初始化
5. 离开页面时通过 `destroyLive2d()` 完整清理
6. 鼠标跟随范围覆盖整个页面，不再局限于画布

## Local Development

### 1. 进入服务器并安装环境

ssh root@你的服务器公网IP
apt update
apt upgrade -y
apt install -y git curl unzip nginx mysql-server openjdk-17-jdk maven
systemctl enable nginx
systemctl start nginx
systemctl enable mysql
systemctl start mysql
java -version
mvn -version
node -v
如果还没装 Node 20，再执行：

curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
node -v
npm -v
### 2. clone 项目

cd /home
git clone https://github.com/AZHI4514/personal-blog.git
cd /home/personal-blog
### 3. 初始化 MySQL

mysql -uroot
进 MySQL 后执行：

CREATE DATABASE IF NOT EXISTS blog_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'blog_user'@'localhost' IDENTIFIED BY '你的数据库密码';
GRANT ALL PRIVILEGES ON blog_db.* TO 'blog_user'@'localhost';
FLUSH PRIVILEGES;
exit;
导入数据库：

mysql -u blog_user -p blog_db < /home/personal-blog/blog_db_backup.sql
### 4. 创建后端运行目录和上传目录

mkdir -p /opt/personal-blog/backend/uploads
mkdir -p /var/www/personal-blog
### 5. 构建前端

cd /home/personal-blog/frontend
npm install
npm run build
rm -rf /var/www/personal-blog/*
cp -r dist/* /var/www/personal-blog/
### 6. 构建后端

cd /home/personal-blog/backend
mvn clean package -DskipTests
cp target/personal-blog-0.0.1-SNAPSHOT.jar /opt/personal-blog/backend/app.jar
### 7. 创建 systemd 后端服务

cat >/etc/systemd/system/personal-blog-back.service <<'EOF'
[Unit]
Description=Personal Blog Backend
After=network.target mysql.service

[Service]
User=root
WorkingDirectory=/opt/personal-blog/backend
Environment="PORT=8080"
Environment="DB_HOST=127.0.0.1"
Environment="DB_PORT=3306"
Environment="DB_NAME=blog_db"
Environment="DB_USERNAME=blog_user"
Environment="DB_PASSWORD=你的数据库密码"
Environment="UPLOAD_PATH=/opt/personal-blog/backend/uploads"
Environment="CORS_ALLOWED_ORIGINS=https://你的域名"
ExecStart=/usr/bin/java -jar /opt/personal-blog/backend/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
### 8. 启动后端

systemctl daemon-reload
systemctl enable personal-blog-back
systemctl start personal-blog-back
systemctl status personal-blog-back
看日志：

journalctl -u personal-blog-back -f

## 游戏角 Agent 配置说明（新增）

本次接入只改了两个位置：

1. 前端只在 `frontend/src/App.vue` 的“游戏角”页面内嵌了 agent 界面，没有新建组件，也没有改动其他页面的结构。
2. 后端新增了独立的 `room-agent` 接口，用来给游戏角提供房间状态、长期记忆和 MCP 白名单调用能力，不影响原有的图片、音乐、BBS、用户等接口。

### 前端是怎么接入的

游戏角页面现在包含这几部分：

- Live2D 展示区：继续复用原来游戏角里的 Live2D 逻辑。
- 对话区：可以直接输入文本，也可以附带图片。
- 房间状态卡：会从后端读取当前房间状态并显示在 Live2D 区域上。
- 音乐信息卡：直接复用现有的音乐列表数据，不新增音乐表。
- 设置面板：也直接写在 `App.vue` 里，没有拆组件。

设置面板里目前可配置：

- `API URL`
- `API Key`
- `模型名`
- `视觉模式`
- 是否启用长期记忆
- 是否启用 MCP
- 角色知识库条目

这些前端设置目前都保存在浏览器本地 `localStorage` 中，使用的 key 是：

- `roomLLMSettings`
- `roomMemorySettings`
- `roomMCPSettings`
- `roomKnowledgeSettings`
- `roomChatHistory`

其中 `API Key` 保留为空，后续在游戏角页面的设置面板里填写即可。

### 后端是怎么接入的

本次新增了两个后端文件：

- `backend/src/main/java/com/azhi/controller/RoomAgentController.java`
- `backend/src/main/java/com/azhi/service/RoomAgentService.java`

并且只额外放开了 `PATCH` 跨域方法，方便记忆接口更新使用。

当前提供的接口如下：

- `GET /room-agent/world`
- `GET /room-agent/memory`
- `POST /room-agent/memory`
- `PATCH /room-agent/memory/{id}`
- `DELETE /room-agent/memory/{id}`
- `POST /room-agent/mcp/call`

### 长期记忆现在的实现方式

按照你的要求，这次**没有新增数据库表**。

当前长期记忆是后端内存版实现，特点是：

- 不需要你改数据库
- 服务重启后记忆会丢失
- 已经保留了后续替换成数据库版本的接口边界

也就是说，你现在可以先把功能跑通；如果以后你想把长期记忆持久化到 MySQL，再单独补表即可。

如果后续你决定改成数据库版，我再给你正式的建表 SQL，你建完我再帮你切过去。

### MCP 现在的实现方式

当前 MCP 接口是白名单占位实现，只允许这两个工具名：

- `understand_image`
- `web_search`

这样做是为了不破坏你原来的后端结构，也避免把敏感调用直接开放到前端。

### LLM 接入方式

当前默认支持两种模式：

1. 前端直接请求你填写的模型接口地址。
2. 勾选“使用服务端代理”后，走 `/api/chat` 代理模式。

注意：

- 这次我没有帮你新增 `/api/chat` 后端代理接口。
- 如果你后面想走代理模式，需要你现有后端再补一个代理接口，或者继续使用直连模式。

所以你现在最简单的用法是：

1. 打开“游戏角”
2. 打开设置面板
3. 填入模型的 `API URL`
4. 填入你自己的 `API Key`
5. 保存后直接聊天

### 这次接入没有改动的内容

为了避免破坏你原有结构，这次没有动这些部分：

- 没有新建前端组件
- 没有改你的路由结构
- 没有改图片页、音乐页、BBS 页、规则页等其他页面
- 没有改你现有数据库表
- 没有替换你原来的音乐、Live2D、用户登录等已有逻辑

### 你后续需要自己补的内容

要让这个 agent 真正接入大模型，你只需要补这一个核心信息：

- 在游戏角设置面板里填写真实的 `API URL` 和 `API Key`

如果你以后想继续增强，可以再做这些扩展：

- 把长期记忆从内存版改成 MySQL 持久化
- 新增后端 `/api/chat` 代理接口
- 把 `MCP` 的占位返回替换成真实工具调用
### 9. 配置 Nginx

cat >/etc/nginx/sites-available/personal-blog <<'EOF'
server {
    listen 80;
    server_name 你的域名;

    root /var/www/personal-blog;
    index index.html;
    client_max_body_size 10m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /posts {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /images {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /musics {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /clap {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /users {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /visitor-stats {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
启用站点：

ln -sf /etc/nginx/sites-available/personal-blog /etc/nginx/sites-enabled/personal-blog
nginx -t
systemctl reload nginx
如果默认站点冲突：

rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
### 10. 开放安全组
放行这些端口：

22
80
443
不要对公网开放：

3306
8080
### 11. 本机检查
检查 MySQL：

mysql -u blog_user -p -h 127.0.0.1 blog_db
检查后端：

curl http://127.0.0.1:8080/posts
curl http://127.0.0.1:8080/images
curl http://127.0.0.1:8080/visitor-stats/total
检查前端：

curl http://127.0.0.1
### 12. 访问
浏览器打开：

http://你的域名
或

http://你的服务器公网IP



### 无域名、直接用公网 IP”的 Nginx 配置


先创建 Nginx 配置：

cat >/etc/nginx/sites-available/personal-blog <<'EOF'
server {
    listen 80;
    server_name _;

    root /var/www/personal-blog;
    index index.html;
    client_max_body_size 10m;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /posts {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /images {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /musics {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /clap {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /users {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /visitor-stats {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
然后启用配置：

ln -sf /etc/nginx/sites-available/personal-blog /etc/nginx/sites-enabled/personal-blog
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
如果你前端已经构建并复制到：

/var/www/personal-blog
那现在直接访问：

http://你的服务器公网IP
就可以了。

顺手检查这几个点：

systemctl status nginx
systemctl status personal-blog-back
curl http://127.0.0.1:8080/posts
curl http://127.0.0.1
如果页面能打开但接口没数据，再看后端日志：

journalctl -u personal-blog-back -f
