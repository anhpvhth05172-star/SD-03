import { reactive, computed, watch } from "vue";

export const money = (n) => n.toLocaleString("vi-VN") + "₫";

export const products = [
  {
    id: 1,
    code: "SP001",
    name: "Air Jordan 1 Retro High",
    brand: "Nike",
    type: "Cao cổ",
    price: 3200000,
    old: 3900000,
    sizes: [39, 40, 41, 42, 43],
    emoji: "👟",
  },
  {
    id: 2,
    code: "SP002",
    name: "Ultraboost Light",
    brand: "Adidas",
    type: "Chạy bộ",
    price: 4100000,
    old: 4500000,
    sizes: [38, 39, 40, 41, 42],
    emoji: "👟",
  },
  {
    id: 3,
    code: "SP003",
    name: "Air Force 1 Low",
    brand: "Nike",
    type: "Thấp cổ",
    price: 2700000,
    old: 0,
    sizes: [38, 39, 40, 41, 42, 43],
    emoji: "👟",
  },
  {
    id: 4,
    code: "SP004",
    name: "Chuck Taylor All Star",
    brand: "Converse",
    type: "Cao cổ",
    price: 1500000,
    old: 1800000,
    sizes: [37, 38, 39, 40, 41],
    emoji: "👟",
  },
  {
    id: 5,
    code: "SP005",
    name: "Old Skool Classic",
    brand: "Vans",
    type: "Thấp cổ",
    price: 1750000,
    old: 0,
    sizes: [38, 39, 40, 41, 42],
    emoji: "👟",
  },
  {
    id: 6,
    code: "SP006",
    name: "Suede Classic XXI",
    brand: "Puma",
    type: "Thấp cổ",
    price: 2100000,
    old: 2400000,
    sizes: [39, 40, 41, 42],
    emoji: "👟",
  },
  {
    id: 7,
    code: "SP007",
    name: "Pegasus 40",
    brand: "Nike",
    type: "Chạy bộ",
    price: 3500000,
    old: 0,
    sizes: [40, 41, 42, 43],
    emoji: "👟",
  },
  {
    id: 8,
    code: "SP008",
    name: "Samba OG",
    brand: "Adidas",
    type: "Thấp cổ",
    price: 2900000,
    old: 3200000,
    sizes: [38, 39, 40, 41, 42],
    emoji: "👟",
  },
];
export const brands = [...new Set(products.map((p) => p.brand))];
export const types = [...new Set(products.map((p) => p.type))];

const saved = (() => {
  try {
    return JSON.parse(localStorage.getItem("ps_cart")) || [];
  } catch {
    return [];
  }
})();
export const cart = reactive({ items: saved });
watch(
  () => cart.items,
  (v) => localStorage.setItem("ps_cart", JSON.stringify(v)),
  { deep: true },
);

export const cartCount = computed(() =>
  cart.items.reduce((s, i) => s + i.qty, 0),
);
export const cartTotal = computed(() =>
  cart.items.reduce((s, i) => s + i.qty * i.price, 0),
);

export function addToCart(p, size, qty = 1) {
  const found = cart.items.find((i) => i.id === p.id && i.size === size);
  if (found) found.qty += qty;
  else
    cart.items.push({
      id: p.id,
      name: p.name,
      brand: p.brand,
      price: p.price,
      emoji: p.emoji,
      size,
      qty,
    });
}
export const removeItem = (idx) => cart.items.splice(idx, 1);
export const clearCart = () => (cart.items = []);

const readOrders = () => {
  try {
    return JSON.parse(localStorage.getItem("ps_orders")) || [];
  } catch {
    return [];
  }
};
export function placeOrder(info) {
  const id = "HD" + Date.now().toString().slice(-6);
  const order = {
    id,
    info,
    items: JSON.parse(JSON.stringify(cart.items)),
    total: cartTotal.value,
    date: new Date().toLocaleString("vi-VN"),
  };
  localStorage.setItem("ps_orders", JSON.stringify([...readOrders(), order]));
  clearCart();
  return id;
}
export const getOrder = (id) => readOrders().find((o) => o.id === id);
