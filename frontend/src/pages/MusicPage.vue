<script setup>
import { useBlogApp } from '@/composables/useBlogApp'

const {
  musicList,
  currentMusicIndex,
  isPlaying,
  currentTime,
  duration,
  searchKeyword,
  filteredMusicList,
  playMusic,
  togglePlay,
  prevTrack,
  nextTrack,
  formatTime
} = useBlogApp()
</script>

<template>
  <div>
          <h1 class="music-title"><span class="music-icon">◆&nbsp;&nbsp;</span>音乐放置处<span class="music-icon"></span>&nbsp;&nbsp;◆</h1>
          <p>收录站长喜欢的曲目，点击列表即可播放。</p>
  
          <!-- 搜索框 -->
          <div class="music-search">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="请输入歌曲名或艺术家" 
              class="search-input"
            >
          </div>
  
          <!-- 播放器控制栏 - 仅当有歌曲被选中时显示 -->
          <div class="music-player" v-if="currentMusicIndex !== -1">
            <div class="player-controls">
              <button @click="prevTrack" class="ctrl-btn">⏮ 上一首</button>
              <button @click="togglePlay" class="ctrl-btn" :class="{ 'playing': isPlaying }">{{ isPlaying ? '⏸ 暂停' : '▶ 播放' }}</button>
              <button @click="nextTrack" class="ctrl-btn">下一首 ⏭</button>
            </div>
            <div class="player-info">
              <!-- 注意：当前歌曲信息从 musicList 中根据索引获取 -->
              <img :src="musicList[currentMusicIndex]?.coverPath" class="player-cover" alt="封面">
              <div class="player-details">
                <div class="player-title">{{ musicList[currentMusicIndex]?.title }} - {{ musicList[currentMusicIndex]?.artist }}</div>
                <div class="player-progress">
                  <span>{{ formatTime(currentTime) }}</span>
                  <progress :value="currentTime" :max="duration" class="progress-bar"></progress>
                  <span>{{ formatTime(duration) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="no-music">
            <p>暂未选择音乐，请从下方列表点击播放～</p>
          </div>
  
          <!-- 曲目列表（不变） -->
          <div class="music-list">
            <h3>曲目列表</h3>
            <table class="music-table">
              <thead>
                <tr><th>序号</th><th>封面</th><th>曲名</th><th>艺术家</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="(music, idx) in filteredMusicList" :key="music.musicId" @click="playMusic(music)" class="music-item">
                  <td>{{ idx + 1 }}</td>
                  <td><img :src="music.coverPath" class="list-cover" alt="封面"></td>
                  <td>{{ music.title }}</td>
                  <td>{{ music.artist }}</td>
                  <td><button class="play-btn">▶ 播放</button></td>
                </tr>
              </tbody>
            </table>
          </div>
          <p>点击曲目即可开始播放♪</p>
        </div>
  
        <!-- 链接集结构 -->
</template>
