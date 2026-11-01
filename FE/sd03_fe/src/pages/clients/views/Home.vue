<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { getSanPhamClient } from "@/api/product";
import ProductCard from "./ProductCard.vue";

const featured = ref([]);
const currentSlide = ref(0);
let bannerTimer = null;

// Banner slider với nội dung chính xác theo ảnh mẫu
const banners = [
  {
    tag: "NEW ARRIVAL",
    titleStart: "BỨT PHÁ PHONG CÁCH",
    titleHighlight: "CÙNG",
    brandHighlight: "NIKE AIR & JORDAN",
    desc: "Đậm chất hiện đại, tinh tế trong từng chi tiết.\nSẵn sàng đồng hành cùng bạn trên mọi hành trình.",
    btnPrimary: "Xem dòng sản phẩm",
    btnPrimaryLink: "/cua-hang?brand=Nike",
    btnSecondary: "Xem tất cả",
    btnSecondaryLink: "/cua-hang",
    image: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=80",
    gradient: "linear-gradient(105deg, #0a0102 0%, #1a0407 35%, #50080f 70%, #880e18 100%)",
    subLabels: ["NIKE", "JORDAN", "AIR MAX", "JUST DO IT"]
  },
  {
    tag: "SPECIAL EDITION",
    titleStart: "CHINH PHỤC TỪNG BƯỚC",
    titleHighlight: "VỚI",
    brandHighlight: "ADIDAS ULTRABOOST",
    desc: "Đệm Boost đàn hồi đỉnh cao, thiết kế ôm chân hoàn hảo.\nTận hưởng độ êm ái vượt trội suốt ngày dài.",
    btnPrimary: "Xem dòng sản phẩm",
    btnPrimaryLink: "/cua-hang?brand=Adidas",
    btnSecondary: "Xem tất cả",
    btnSecondaryLink: "/cua-hang",
    image: "https://images.unsplash.com/photo-1552346154-21d32810aba3?auto=format&fit=crop&w=1000&q=80",
    gradient: "linear-gradient(105deg, #030712 0%, #111827 40%, #1f293d 75%, #374151 100%)",
    subLabels: ["ADIDAS", "ULTRABOOST", "ORIGINALS", "SUPERSTAR"]
  }
];

const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % banners.length;
};

const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + banners.length) % banners.length;
};

const goToSlide = (index) => {
  currentSlide.value = index;
  resetTimer();
};

const resetTimer = () => {
  if (bannerTimer) clearInterval(bannerTimer);
  bannerTimer = setInterval(nextSlide, 5000);
};

// 7 thương hiệu nổi bật có logo/icon hiển thị dạng card trắng như ảnh mẫu
const featuredBrands = [
  { name: "Nike", icon: "bi-check2", link: "/cua-hang?brand=Nike", isNike: true },
  { name: "Jordan", icon: "bi-person-arms-up", link: "/cua-hang?brand=Jordan", isJumpman: true },
  { name: "MLB", text: "⚾ MLB", link: "/cua-hang?brand=MLB" },
  { name: "Lacoste", text: "🐊", link: "/cua-hang?brand=Lacoste" },
  { name: "Salomon", iconText: "S", nameText: "Salomon", link: "/cua-hang?brand=Salomon" },
  { name: "On Running", text: "8", nameText: "On Running", link: "/cua-hang?brand=On" },
  { name: "Under Armour", iconText: "X", nameText: "Under Armour", link: "/cua-hang?brand=Under Armour" },
];

onMounted(async () => {
  resetTimer();
  try {
    const res = await getSanPhamClient({ page: 0, size: 8 });
    const list = res?.content ?? res ?? [];
    featured.value = list.slice(0, 8);
  } catch {
    // fallback
  }
});

onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer);
});
</script>

<template>
  <div class="ps-home-container">
    <!-- 1. BANNER HERO SLIDER ĐÚNG MẪU -->
    <section class="banner-carousel position-relative mb-5 overflow-hidden">
      <div 
        class="banner-slides-wrapper" 
        :style="{ transform: `translateX(-${currentSlide * 100}%)` }"
      >
        <div 
          v-for="(banner, idx) in banners" 
          :key="idx" 
          class="banner-slide"
          :style="{ background: banner.gradient }"
        >
          <div class="container h-100 position-relative">
            <div class="row h-100 align-items-center g-4 py-4 py-lg-5">
              <!-- Cột trái: Văn bản -->
              <div class="col-lg-6 text-white text-content z-2 ps-lg-4">
                <div class="banner-top-tag mb-2 d-flex align-items-center gap-2">
                  <span class="text-white-50 small fw-bold tracking-widest">{{ banner.tag }}</span>
                  <span class="tag-dash"></span>
                </div>
                
                <h1 class="banner-title text-white mb-3">
                  {{ banner.titleStart }}<br />
                  <span class="text-white fw-bold">{{ banner.titleHighlight }}</span>
                  <span class="text-danger fw-black ms-2">{{ banner.brandHighlight }}</span>
                </h1>

                <p class="banner-desc text-white-50 mb-4" style="white-space: pre-line;">
                  {{ banner.desc }}
                </p>

                <div class="d-flex flex-wrap gap-3 align-items-center">
                  <RouterLink 
                    :to="banner.btnPrimaryLink" 
                    class="btn btn-danger banner-btn-primary rounded-pill px-4 py-2 d-inline-flex align-items-center gap-2 fw-bold"
                  >
                    <span>{{ banner.btnPrimary }}</span>
                    <i class="bi bi-arrow-right"></i>
                  </RouterLink>
                  <RouterLink 
                    :to="banner.btnSecondaryLink" 
                    class="btn btn-outline-light banner-btn-secondary rounded-pill px-4 py-2 fw-semibold"
                  >
                    {{ banner.btnSecondary }}
                  </RouterLink>
                </div>
              </div>

              <!-- Cột phải: Ảnh giày đỏ nổi bật và nhãn thương hiệu -->
              <div class="col-lg-6 text-center position-relative banner-img-wrap">
                <div class="glow-radial"></div>
                <img 
                  :src="banner.image" 
                  :alt="banner.brandHighlight" 
                  class="banner-hero-img img-fluid"
                  loading="lazy"
                />

                <!-- Text nhãn dọc bên phải theo thiết kế ảnh mẫu -->
                <div class="banner-right-labels d-none d-lg-flex flex-column text-white-50">
                  <span v-for="lbl in banner.subLabels" :key="lbl" class="banner-sub-lbl">
                    {{ lbl }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Nút tròn chuyển slide mũi tên trái/phải -->
      <button 
        class="banner-nav-btn prev-btn" 
        @click="prevSlide" 
        aria-label="Slide trước"
      >
        <i class="bi bi-chevron-left"></i>
      </button>
      <button 
        class="banner-nav-btn next-btn" 
        @click="nextSlide" 
        aria-label="Slide sau"
      >
        <i class="bi bi-chevron-right"></i>
      </button>

      <!-- Dots chuyển slide -->
      <div class="banner-dots">
        <button
          v-for="(_, i) in banners"
          :key="i"
          class="banner-dot"
          :class="{ active: currentSlide === i }"
          @click="goToSlide(i)"
          :aria-label="'Chuyển đến slide ' + (i + 1)"
        ></button>
      </div>
    </section>

    <!-- 2. THƯƠNG HIỆU HÀNG ĐẦU (7 Ô Card trắng bo viền theo ảnh mẫu) -->
    <section class="mb-5">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <div>
          <div class="section-tag small fw-bold text-danger d-flex align-items-center gap-1">
            <span class="slash-mark">/</span> THƯƠNG HIỆU ĐỒNG HÀNH
          </div>
          <h4 class="fw-black text-dark mb-0 mt-1">Những thương hiệu hàng đầu</h4>
        </div>
        <RouterLink to="/cua-hang" class="text-danger fw-semibold small text-decoration-none">
          Xem tất cả <i class="bi bi-arrow-right"></i>
        </RouterLink>
      </div>

      <div class="row g-2 g-md-3">
        <div v-for="b in featuredBrands" :key="b.name" class="col-4 col-md-3 col-lg">
          <RouterLink :to="b.link" class="brand-showcase-card text-decoration-none text-dark">
            <div class="brand-logo-area">
              <!-- Nike Swoosh -->
              <svg v-if="b.isNike" class="brand-svg-icon" viewBox="0 0 24 24" fill="currentColor">
                <path d="M21.707 5.293c-.27-.27-.69-.33-1.02-.15l-14 8a1.002 1.002 0 0 0-.47.67c-.07.36.05.73.32.98l4 3.73c.18.17.43.27.67.27.18 0 .36-.05.52-.16l9.98-7.06c.35-.25.46-.73.27-1.11-.19-.38-.66-.54-1.05-.35l-9.17 6.49-2.73-2.55 12.68-7.77z"/>
              </svg>
              <!-- Jumpman Icon -->
              <i v-else-if="b.isJumpman" class="bi bi-person-walking fs-3"></i>
              <!-- Text brand -->
              <span v-else class="fw-bold fs-6">{{ b.text || b.nameText }}</span>
            </div>
            <div v-if="b.nameText" class="brand-subtext">{{ b.nameText }}</div>
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- 3. PROMO 2 BANNER (Streetwear 2026 & Giày chạy bộ - Đúng mẫu ảnh) -->
    <section class="row g-4 mb-5">
      <!-- Banner trái: Streetwear 2026 nền trắng -->
      <div class="col-md-6">
        <div class="promo-card-custom promo-card-streetwear p-4 rounded-4 position-relative overflow-hidden d-flex align-items-center">
          <div class="promo-text-col z-2">
            <span class="badge bg-danger rounded-pill px-2 py-1 small fw-bold mb-2">HOT TREND</span>
            <h4 class="fw-black text-dark mb-1">Sneaker Streetwear 2026</h4>
            <p class="text-muted-ps small mb-3">Thời trang hiện đại, phong cách trẻ trung năng động.</p>
            <RouterLink to="/cua-hang" class="btn btn-danger btn-sm rounded-pill px-3 fw-bold">
              Mua Ngay <i class="bi bi-arrow-right"></i>
            </RouterLink>
          </div>
          <div class="promo-img-col">
            <img 
              src="https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=500&q=80" 
              alt="Streetwear Sneaker" 
              class="promo-shoe-img"
            />
          </div>
        </div>
      </div>

      <!-- Banner phải: Giày Chạy Bộ & Thể Thao nền tối đỏ -->
      <div class="col-md-6">
        <div class="promo-card-custom promo-card-running p-4 rounded-4 position-relative overflow-hidden d-flex align-items-center">
          <div class="promo-text-col z-2">
            <span class="badge bg-danger rounded-pill px-2 py-1 small fw-bold mb-2">GIẢM ĐẾN 30%</span>
            <h4 class="fw-black text-white mb-1">Giày Chạy Bộ & Thể Thao</h4>
            <p class="text-white-50 small mb-3">Công nghệ tiên tiến, bứt phá từng bước chân.</p>
            <RouterLink to="/cua-hang" class="btn btn-light btn-sm rounded-pill px-3 fw-bold text-dark">
              Xem Ngay <i class="bi bi-arrow-right"></i>
            </RouterLink>
          </div>
          <div class="promo-img-col">
            <img 
              src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=500&q=80" 
              alt="Running Sneaker" 
              class="promo-shoe-img"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- 4. SẢN PHẨM NỔI BẬT (Heading & Link khám phá bộ sưu tập) -->
    <section class="mb-5">
      <div class="d-flex justify-content-between align-items-end mb-4">
        <div>
          <div class="section-tag small fw-bold text-danger d-flex align-items-center gap-1">
            <span class="slash-mark">/</span> TUYỂN CHỌN <span class="text-danger">HÀNG ĐẦU</span>
          </div>
          <h3 class="fw-black text-dark mb-0 mt-1">Sản phẩm nổi bật</h3>
        </div>
        <RouterLink 
          to="/cua-hang" 
          class="text-danger fw-semibold small text-decoration-none"
        >
          Khám phá bộ sưu tập <i class="bi bi-arrow-right"></i>
        </RouterLink>
      </div>

      <div v-if="featured.length" class="row g-3 g-md-4">
        <div v-for="p in featured" :key="p.id" class="col-6 col-md-4 col-lg-3">
          <ProductCard :product="p" />
        </div>
      </div>
      <div v-else class="text-center py-5 ps-card">
        <div class="fs-1 mb-2">👟</div>
        <h5 class="fw-bold">Đang cập nhật sản phẩm...</h5>
        <p class="text-muted small">Mời bạn quay lại danh mục sản phẩm sau nhé!</p>
      </div>
    </section>

    <!-- 5. KHỐI TRẢI NGHIỆM MUA SẮM (Banner đỏ nhỏ bên trái + 4 cam kết bên phải - Theo đúng ảnh) -->
    <section class="mb-5">
      <div class="experience-strip rounded-4 p-4 p-md-4 bg-white border">
        <div class="row align-items-center g-4">
          <!-- Cột đỏ nhỏ giày bên trái -->
          <div class="col-lg-5 d-flex align-items-center gap-3">
            <div class="experience-thumb-box rounded-3 overflow-hidden flex-shrink-0">
              <img 
                src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=300&q=80" 
                alt="Sneaker Shoes" 
                class="experience-thumb-img"
              />
            </div>
            <div>
              <div class="small fw-bold text-danger text-uppercase mb-1">/ VÌ SAO CHỌN POLY SHOES?</div>
              <h4 class="fw-black text-dark mb-0 lh-sm">
                Trải nghiệm mua sắm<br />sneaker đơn giản và an toàn
              </h4>
            </div>
          </div>

          <!-- 4 cột cam kết bên phải đúng ảnh -->
          <div class="col-lg-7">
            <div class="row g-3 text-start">
              <div class="col-6 col-md-3">
                <div class="exp-perk-item">
                  <div class="exp-icon-shield text-danger mb-2">
                    <i class="bi bi-shield-check fs-4"></i>
                  </div>
                  <div class="fw-bold small text-dark">Chính hãng 100%</div>
                  <p class="text-muted-ps extra-small mb-0">Cam kết nguồn gốc, hoàn tiền nếu phát hiện hàng giả.</p>
                </div>
              </div>
              <div class="col-6 col-md-3">
                <div class="exp-perk-item">
                  <div class="exp-icon-shield text-danger mb-2">
                    <i class="bi bi-arrow-repeat fs-4"></i>
                  </div>
                  <div class="fw-bold small text-dark">Đổi size trong 7 ngày</div>
                  <p class="text-muted-ps extra-small mb-0">Hỗ trợ đổi miễn phí lần đầu nếu không vừa.</p>
                </div>
              </div>
              <div class="col-6 col-md-3">
                <div class="exp-perk-item">
                  <div class="exp-icon-shield text-danger mb-2">
                    <i class="bi bi-credit-card-2-front fs-4"></i>
                  </div>
                  <div class="fw-bold small text-dark">Thanh toán an toàn</div>
                  <p class="text-muted-ps extra-small mb-0">Đa dạng hình thức, bảo mật tuyệt đối.</p>
                </div>
              </div>
              <div class="col-6 col-md-3">
                <div class="exp-perk-item">
                  <div class="exp-icon-shield text-danger mb-2">
                    <i class="bi bi-headset fs-4"></i>
                  </div>
                  <div class="fw-bold small text-dark">Hỗ trợ 24/7</div>
                  <p class="text-muted-ps extra-small mb-0">Tư vấn nhanh chóng, chuyên nghiệp.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Hero Carousel */
.banner-carousel {
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.banner-slides-wrapper {
  display: flex;
  transition: transform 0.6s cubic-bezier(0.25, 1, 0.5, 1);
  width: 100%;
}

.banner-slide {
  min-width: 100%;
  box-sizing: border-box;
  min-height: 440px;
  display: flex;
  align-items: center;
}

@media (min-width: 768px) {
  .banner-slide {
    min-height: 480px;
  }
}

.banner-top-tag {
  letter-spacing: 0.15em;
}

.tag-dash {
  display: inline-block;
  width: 32px;
  height: 2px;
  background-color: #ef4444;
}

.banner-title {
  font-size: 2.5rem;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

@media (min-width: 992px) {
  .banner-title {
    font-size: 3rem;
  }
}

.banner-desc {
  max-width: 460px;
  line-height: 1.6;
  font-size: 0.95rem;
}

.banner-btn-primary {
  background-color: #d32f2f !important;
  border-color: #d32f2f !important;
  font-size: 0.9rem;
}

.banner-btn-secondary {
  border-color: rgba(255, 255, 255, 0.4) !important;
  font-size: 0.9rem;
}

.banner-btn-secondary:hover {
  background-color: rgba(255, 255, 255, 0.15) !important;
}

/* Image Column */
.banner-img-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.glow-radial {
  position: absolute;
  width: 380px;
  height: 380px;
  background: radial-gradient(circle, rgba(239, 68, 68, 0.35) 0%, rgba(0, 0, 0, 0) 70%);
  border-radius: 50%;
  pointer-events: none;
}

.banner-hero-img {
  max-height: 360px;
  width: auto;
  object-fit: contain;
  transform: rotate(-18deg);
  filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.5));
  transition: transform 0.4s ease;
}

.banner-slide:hover .banner-hero-img {
  transform: rotate(-14deg) scale(1.04);
}

.banner-right-labels {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  gap: 12px;
  text-align: right;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.12em;
}

/* Nav & Dots */
.banner-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s ease;
}

.banner-nav-btn:hover {
  background: #d32f2f;
  border-color: #d32f2f;
}

.prev-btn { left: 16px; }
.next-btn { right: 16px; }

.banner-dots {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 10;
}

.banner-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.4);
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
}

.banner-dot.active {
  width: 22px;
  background: #d32f2f;
}

/* Brand Showcase Cards */
.brand-showcase-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.25rem 0.5rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 84px;
  transition: all 0.25s ease;
}

.brand-showcase-card:hover {
  border-color: #d32f2f;
  box-shadow: 0 6px 16px rgba(211, 47, 47, 0.1);
  transform: translateY(-3px);
}

.brand-logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
}

.brand-svg-icon {
  width: 38px;
  height: 38px;
}

.brand-subtext {
  font-size: 0.68rem;
  color: #64748b;
  margin-top: 4px;
  font-weight: 600;
}

/* Promo Custom Cards */
.promo-card-custom {
  min-height: 180px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.promo-card-custom:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08);
}

.promo-card-streetwear {
  background: #ffffff;
}

.promo-card-running {
  background: linear-gradient(105deg, #09090b 0%, #18181b 40%, #580c14 100%);
  border-color: #27272a;
}

.promo-shoe-img {
  position: absolute;
  right: 10px;
  bottom: 10px;
  width: 220px;
  height: auto;
  object-fit: contain;
  transform: rotate(-10deg);
  transition: transform 0.3s ease;
}

.promo-card-custom:hover .promo-shoe-img {
  transform: rotate(-6deg) scale(1.05);
}

.slash-mark {
  font-weight: 900;
  font-size: 1.1rem;
}

/* Experience Strip */
.experience-strip {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.experience-thumb-box {
  width: 130px;
  height: 95px;
  background: #fee2e2;
}

.experience-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.extra-small {
  font-size: 0.72rem;
  line-height: 1.35;
}

.exp-icon-shield {
  display: inline-block;
}
</style>
