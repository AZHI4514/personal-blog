<script setup>
import { useBlogApp } from '@/composables/useBlogApp'

const {
  visitorCount,
  handleClap,
  showPage
} = useBlogApp()
</script>

<template>
  <div>
          <div class="header-section">
            <div class="header-top">
              <img src="@/assets/images/title_text.png" style="width: 1000px; height: 150px;" alt="标题文本图" class="title-img">
            </div>
            <p class="welcome-msg">★★★ 欢迎光临！本网站是以anime为主的插画交流网站 ★★★</p>
          </div>
          <div class="warning-bar">
              <div class="warning-bar-marquee-inner">
                  <span class="warning-bar-text">
                      <span class="star-orange">★</span>&thinsp;<span class="star-green">★</span>&thinsp;<span class="star-purple">★</span>
                      &thinsp;&thinsp;<strong>未经许可禁止转载/复制</strong>&thinsp;&thinsp;
                      <span class="star-purple">★</span>&thinsp;<span class="star-green">★</span>&thinsp;<span class="star-orange">★</span>
                  </span>
              </div>
          </div>
          <div class="counter-section">
            <div>
              您是第
                <span class="counter-digital">{{ visitorCount }}
                </span>
              位到访的客人！
            </div>
            <div class="counter-info">
              若踩到整数号，请通过 <a href="#" @click="handleClap">拍手</a>
              或 <a href="#" @click.prevent="showPage('bbs')">BBS留言</a> 告诉我～
            </div>
          </div>
          <div class="center-area">
            <div class="illust-container">
              <img src="@/assets/images/main_illust.jpg" alt="本月插画" class="main-illust" width="650" height="488" fetchpriority="high">
              <div class="illust-credit">本月插画 / 头像：站主</div>
            </div>
            <div class="log-box">
              <div class="log-content">
                <ul>
                  <li><span class="log">2026-6-5 </span> 加入了聊天助手　☽月見八千代☾　！</li>
                  <li><span class="log">2026-5-30</span> 更新了音乐与画廊样式~</li>
                  <li><span class="log">2026-5-29</span> 更新了☽AZHI☾的小屋</li>
                  <li><span class="log">2026-4-19</span> 已修复Links☆</li>
                  <li><span class="log">2026-4-19</span> 留言板垃圾信息过滤加强！</li>
                  <li><span class="log">2026-4-17</span> 更新了个人资料100问100答♪</li>
                  <li><span class="log">2026-3-16</span> 更换了网站背景 (>_<)</li>
                  <li><span class="log">2026-3-15</span> 已把画作全部备份完成！
                  </li>
                </ul>
              </div>
                <div class="clap-section" style="padding-top:15px;">
                  <div class="tooltip-container btn-mode">
                    <button class="btn-clap" @click="handleClap">拍手!</button>
                    <div class="clap-info">★什么是拍手？<br>只要按一下按钮，就可以把应援讯息发送给站长!♪</div>
                  </div>
                  <div class="clap-sub-info">★非常期待大家的应援与留言★</div>
                </div>
            </div>
          </div>
          <div class="notice-box">
            <div class="notice-title">◆ 公告板 ◆</div>
              <div class="notice-content">
                天气渐渐变热了，神明大人们最近还过得好吗？<br>
                （站长因为是重度家里蹲，所以完全不知道外面的气温呢＞＜；）<br>
                近期计划对网站进行改版！<br>
                时间尚未确定，临近时会再来这里通知大家。<br>
                随时欢迎到BBS留言♪
              </div>
          </div>
            <table class="info-table">
              <thead>
                <tr>
                  <th>页面</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><a href="#" @click.prevent="showPage('home')">首页</a></td>
                  <td>就是这里～♪</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('profile')">个人资料</a></td>
                  <td>看完这里，你就能了解站长的一切！？ 一定要看☆</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('gallery')">画廊</a><span class="new-tag">new</span></td>
                  <td>展示着各位神明大人送来的超棒插画！一定要来看看哦</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('bbs')">BBS/留言板</a></td>
                  <td>意见、感想请到留言板♪ 在涂鸦留言板也在征集插画中！一直以来谢谢大家＞＜</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('rules')">使用规定</a></td>
                  <td>一定要先读哦☆</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('games')">游戏角</a> <span class="new-tag">new</span></td>
                  <td>放着站主自己做的游戏，以及觉得很有趣的游戏介绍～♪ &nbsp;正在制作</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('music')">音乐</a></td>
                  <td>收录anime相关曲目，点击后就能直接播放♪</td>
                </tr>
                <tr>
                  <td><a href="#" @click.prevent="showPage('links')">链接集</a></td>
                  <td>非常欢迎互换链接☆<br><img src="@/assets/images/banner.png" style="width: 10%" alt="网站横幅"></td>
                </tr>
              </tbody>
            </table>   
        </div>
        
        <!-- 个人资料页结构 -->
</template>
