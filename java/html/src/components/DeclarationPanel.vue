<script setup>
import { computed } from 'vue'

const props = defineProps({
  godMode: { type: Boolean, default: false }
})
const emit = defineEmits(['set-form'])

const slogan = computed(() => (props.godMode ? '伪神临世' : '我自为神'))
</script>

<template>
  <div class="declaration">
    <div class="verdict">神堂判定：神弃之人 · 终生无法镶嵌神印</div>

    <h1>
      <span class="name">陈 昊 宇</span>
      <span class="eyebrow">觉 醒 伪 神</span>
      <span class="line1">神明弃我<span class="comma">，</span></span>
      <span class="line2" :class="{ god: godMode }">{{ slogan }}</span>
    </h1>

    <!-- 双形态切换 -->
    <div class="forms">
      <div
        class="form-card"
        :class="{ active: !godMode }"
        @click="emit('set-form', false)"
      >
        <div class="tag">第一变 · 恶鬼形态</div>
        <h3>恶鬼</h3>
        <p>黑角 · 赤瞳 · 骨翼<br />力量暴涨，吞噬灵能即愈伤</p>
        <span class="roman">Ⅰ</span>
      </div>
      <div
        class="form-card"
        :class="{ active: godMode }"
        @click="emit('set-form', true)"
      >
        <div class="tag">第二变 · 伪神形态</div>
        <h3>伪神</h3>
        <p>额生银纹 · 瞳作星环<br />复制并篡改他人神技</p>
        <span class="roman">Ⅱ</span>
      </div>
    </div>

    <p class="creed">
      神说他是弃子，<em>恶鬼之牙</em>便倾尽仓库为他铺路。<br />
      神不给我的，<em>我自取之</em>；神嫌弃的，我将其升华为<em>禁忌</em>。
    </p>
  </div>
</template>

<style scoped>
.declaration {
  flex: 1.2;
  user-select: none;
}
.verdict {
  display: inline-block;
  font-size: 12px;
  letter-spacing: .35em;
  color: #9a917f;
  border: 1px dashed #55503f;
  padding: 6px 12px 6px 16px;
  margin-bottom: 24px;
  transform: rotate(-1.2deg);
  position: relative;
}
.verdict::before {
  content: "✕";
  color: var(--c1);
  position: absolute;
  right: -14px;
  top: -12px;
  font-size: 20px;
  text-shadow: 0 0 10px var(--c1-glow);
  transition: color .8s;
}
h1 {
  font-size: clamp(36px, 5vw, 72px);
  line-height: 1.3;
  font-weight: 900;
  letter-spacing: .1em;
}
h1 .name {
  display: block;
  font-size: .42em;
  letter-spacing: .6em;
  color: #9a917f;
  margin-bottom: 14px;
  font-weight: 400;
}

/* 主标：觉醒伪神 */
h1 .eyebrow {
  display: inline-block;
  font-size: .3em;
  letter-spacing: .5em;
  text-indent: .5em;
  color: var(--c1);
  border: 1px solid var(--c1-dim);
  padding: 6px 10px 6px 16px;
  margin-bottom: 20px;
  text-shadow: 0 0 14px var(--c1-glow);
  transition: color .8s, border-color .8s;
}

h1 .line1 { display: block; color: #8f8776; }
h1 .comma {
  color: var(--c1);
  text-shadow: 0 0 16px var(--c1-glow);
  transition: color .8s;
}
h1 .line2 {
  display: block;
  margin-top: 6px;
  color: var(--c1);
  text-shadow: 0 0 24px var(--c1-glow);
  transition: color .8s, text-shadow .8s;
}

.forms {
  margin-top: 34px;
  display: flex;
  gap: 16px;
  max-width: 500px;
}
.form-card {
  flex: 1;
  border: 1px solid #3a3342;
  border-radius: 4px;
  padding: 16px 18px;
  cursor: pointer;
  background: rgba(0, 0, 0, .35);
  transition: all .4s;
  position: relative;
  overflow: hidden;
}
.form-card .tag {
  font-size: 11px;
  letter-spacing: .35em;
  color: #7d7466;
  margin-bottom: 8px;
  transition: color .4s;
}
.form-card h3 {
  font-size: 18px;
  letter-spacing: .25em;
  margin-bottom: 6px;
  color: #b9b1a3;
  transition: color .4s;
}
.form-card p {
  font-size: 11.5px;
  line-height: 1.8;
  color: #6d6660;
  letter-spacing: .08em;
}
.form-card.active {
  border-color: var(--c1);
  box-shadow: 0 0 24px var(--c1-soft), inset 0 0 30px var(--c1-soft);
  transform: translateY(-3px);
}
.form-card.active h3 { color: var(--c1); text-shadow: 0 0 12px var(--c1-glow); }
.form-card.active .tag { color: var(--c1); }
.form-card .roman {
  position: absolute;
  right: 8px;
  bottom: -6px;
  font-size: 52px;
  font-weight: 900;
  color: rgba(255, 255, 255, .04);
  font-family: Georgia, serif;
}

.creed {
  margin-top: 28px;
  font-size: 14px;
  line-height: 2.1;
  color: #938a96;
  letter-spacing: .2em;
  border-left: 2px solid var(--c1-dim);
  padding-left: 20px;
  max-width: 480px;
  transition: border-color .8s;
}
.creed em {
  font-style: normal;
  color: var(--c1);
  transition: color .8s;
}

@media (max-width: 940px) {
  .declaration { text-align: center; }
  .forms { margin: 34px auto 0; }
  .creed { border-left: none; padding-left: 0; margin: 28px auto 0; }
}
</style>
