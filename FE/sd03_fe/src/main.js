import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "./assets/main.css";
import "./assets/screens.css";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "bootstrap";
import "bootstrap-icons/font/bootstrap-icons.css";
import "./assets/styles/custom-swal.css";
import { caiDatFetchAuth } from "./api/fetch-auth";

caiDatFetchAuth();

const app = createApp(App);

app.use(router);

app.mount("#app");
