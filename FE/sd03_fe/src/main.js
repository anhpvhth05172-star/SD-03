import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./assets/screens.css";
import "./assets/main.css";
import "./assets/styles/custom-swal.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import { caiDatFetchAuth } from "./api/fetch-auth";

caiDatFetchAuth();

if (typeof document !== 'undefined') {
  document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
  document.querySelectorAll('vue-devtools-host, #vue-devtools-anchor').forEach(el => el.remove());
  document.body.classList.remove('modal-open', 'swal2-shown', 'swal2-height-auto');
}

const app = createApp(App);

app.use(router);

app.mount("#app");
