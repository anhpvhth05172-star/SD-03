<script setup>
import { ref, onMounted } from "vue";
import { getSanPhamClient } from "@/api/product";
import ProductCard from "./ProductCard.vue";

const featured = ref([]);
const brands = ref([]);

onMounted(async () => {
  try {
    const res = await getSanPhamClient({ page: 0, size: 8 });
    const list = res?.content ?? res ?? [];
    featured.value = list.slice(0, 4);
    brands.value = [...new Set(list.map((p) => p.tenThuongHieu).filter(Boolean))];
  } catch {
    // Giữ bố cục trang ổn định nếu API tạm thời không khả dụng.
  }
});

const perks = [
  { icon: "bi-patch-check", t: "Sản phẩm chính hãng", d: "An tâm chọn lựa, chất lượng được cam kết." },
  { icon: "bi-arrow-repeat", t: "Đổi size dễ dàng", d: "Hỗ trợ đổi size trong 7 ngày." },
  { icon: "bi-truck", t: "Giao hàng toàn quốc", d: "Nhận hàng và kiểm tra trước khi thanh toán." },
];
</script>

<template>
  <section class="home-hero mb-5">
    <div class="home-hero-copy">
      <span class="home-eyebrow"><i class="bi bi-stars me-2"></i>POLY SHOES · EVERYDAY ESSENTIALS</span>
      <h1>Chọn đôi giày.<br /><span>Đi theo cách bạn.</span></h1>
      <p>Những thiết kế sneaker năng động, dễ phối đồ và sẵn sàng đồng hành cùng bạn mỗi ngày.</p>
      <div class="d-flex flex-wrap align-items-center gap-3">
        <RouterLink to="/cua-hang" class="btn btn-primary btn-lg px-4">Khám phá cửa hàng <i class="bi bi-arrow-right ms-2"></i></RouterLink>
        <span class="home-caption"><i class="bi bi-box-seam me-1"></i> Giao hàng toàn quốc</span>
      </div>
    </div>
    <div class="home-hero-art" aria-label="Bộ sưu tập giày Poly Shoes">
      <div class="hero-orbit hero-orbit-one"></div><div class="hero-orbit hero-orbit-two"></div>
      <span class="hero-stamp">MOVE<br />YOUR<br />WAY</span>
      <div class="hero-shoe">👟</div>
      <span class="hero-art-note">NEW<br />SEASON</span>
      <span class="hero-art-index">01 / 26</span>
    </div>
  </section>

  <section v-if="brands.length" class="brand-section mb-5">
    <div class="section-kicker">THƯƠNG HIỆU ĐƯỢC YÊU THÍCH</div>
    <div class="brand-list">
      <RouterLink v-for="b in brands" :key="b" :to="{ path: '/cua-hang', query: { brand: b } }" class="brand-chip">{{ b }}<i class="bi bi-arrow-up-right"></i></RouterLink>
      <RouterLink to="/cua-hang" class="brand-chip brand-chip-all">Tất cả sản phẩm <i class="bi bi-arrow-right"></i></RouterLink>
    </div>
  </section>

  <section v-if="featured.length" class="mb-5">
    <div class="shop-section-heading">
      <div><div class="section-kicker">LỰA CHỌN DÀNH CHO BẠN</div><h2>Sản phẩm nổi bật</h2></div>
      <RouterLink to="/cua-hang" class="section-link">Xem tất cả <i class="bi bi-arrow-right ms-1"></i></RouterLink>
    </div>
    <div class="row g-3 g-lg-4">
      <div v-for="p in featured" :key="p.id" class="col-6 col-lg-3"><ProductCard :product="p" /></div>
    </div>
  </section>

  <section class="perks-strip row g-3">
    <div v-for="x in perks" :key="x.t" class="col-md-4">
      <div class="perk-card h-100"><span class="perk-icon"><i :class="`bi ${x.icon}`"></i></span><div><h3>{{ x.t }}</h3><p>{{ x.d }}</p></div><i class="bi bi-arrow-up-right perk-arrow"></i></div>
    </div>
  </section>
</template>

<style scoped>
.home-hero{min-height:410px;display:grid;grid-template-columns:1.05fr .95fr;overflow:hidden;border-radius:24px;background:#b8202e;color:#fff;position:relative;isolation:isolate}.home-hero-copy{padding:clamp(2rem,5vw,4.5rem);align-self:center;z-index:2}.home-eyebrow{font-size:.72rem;letter-spacing:.15em;font-weight:800;color:#ffe0df}.home-hero h1{font-size:clamp(2.7rem,5vw,4.6rem);line-height:1.02;letter-spacing:-.06em;margin:1.1rem 0;font-weight:850}.home-hero h1 span{color:#ffd7d5}.home-hero-copy p{font-size:1.05rem;line-height:1.7;color:#ffe8e6;max-width:470px;margin-bottom:1.5rem}.home-hero .btn{background:#fff;color:#a61f2b;border:0;box-shadow:none;border-radius:10px;font-weight:750}.home-caption{font-size:.8rem;color:#ffe0df}.home-hero-art{position:relative;display:grid;place-items:center;min-height:360px;background:radial-gradient(ellipse at 50% 50%,#f56c67 0,rgba(245,108,103,.28) 36%,transparent 69%)}.hero-orbit{position:absolute;border:1px solid rgba(255,255,255,.25);border-radius:50%;width:360px;height:360px}.hero-orbit-two{width:275px;height:275px;border-style:dashed}.hero-shoe{font-size:clamp(8rem,18vw,14rem);filter:drop-shadow(0 24px 15px rgba(84,0,8,.25));transform:rotate(-18deg);z-index:1}.hero-stamp{position:absolute;left:10%;top:20%;font-size:.72rem;line-height:1.2;letter-spacing:.15em;font-weight:900;transform:rotate(-12deg);color:#ffd8d5}.hero-art-note{position:absolute;right:13%;top:22%;font-size:.8rem;line-height:1.1;font-weight:900;letter-spacing:.12em;border:1px solid rgba(255,255,255,.65);padding:.55rem;transform:rotate(8deg)}.hero-art-index{position:absolute;right:8%;bottom:9%;font-size:.7rem;letter-spacing:.15em;color:#ffe0df}.brand-section{padding:1.4rem 0;border-bottom:1px solid #eceef2}.section-kicker{font-size:.7rem;letter-spacing:.13em;font-weight:800;color:#9b1c28;margin-bottom:.55rem}.brand-list{display:flex;gap:.65rem;overflow-x:auto;padding:.25rem 0}.brand-chip{display:flex;align-items:center;gap:1rem;white-space:nowrap;padding:.7rem 1rem;border:1px solid #e9e9ec;border-radius:9px;background:#fff;color:#26303d;text-decoration:none;font-size:.88rem;font-weight:700;transition:.18s}.brand-chip i{color:#bd2632}.brand-chip:hover{border-color:#c62828;color:#a61f1f;transform:translateY(-2px)}.brand-chip-all{background:#fff5f4;border-color:#f2d1d0;color:#a61f1f}.shop-section-heading{display:flex;justify-content:space-between;align-items:end;margin-bottom:1.1rem}.shop-section-heading h2{font-size:1.8rem;margin:0;letter-spacing:-.04em}.section-link{color:#a61f1f;font-weight:700;text-decoration:none}.perk-card{position:relative;display:flex;align-items:center;gap:1rem;background:#fff;border:1px solid #eeeef1;border-radius:13px;padding:1.15rem}.perk-icon{width:44px;height:44px;border-radius:12px;background:#fff0ef;color:#b8202e;display:grid;place-items:center;font-size:1.25rem;flex-shrink:0}.perk-card h3{font-size:.92rem;font-weight:800;margin:0 0 .25rem;letter-spacing:0}.perk-card p{font-size:.78rem;color:#747b87;margin:0}.perk-arrow{margin-left:auto;color:#c62828;align-self:flex-start}@media(max-width:767.98px){.home-hero{grid-template-columns:1fr;min-height:unset}.home-hero-copy{padding:2rem 1.5rem 1rem}.home-hero h1{font-size:3rem}.home-hero-art{min-height:230px}.hero-shoe{font-size:9rem}.hero-orbit{width:240px;height:240px}.hero-orbit-two{width:185px;height:185px}.hero-stamp{left:12%;top:15%}.hero-art-note{right:12%;top:14%}.shop-section-heading h2{font-size:1.45rem}}
</style>
