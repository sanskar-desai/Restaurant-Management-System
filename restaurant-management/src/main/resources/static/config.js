/* =========================================
   API BASE URL CONFIGURATION
========================================= */

const API_BASE_URL =
  window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8080"
    : window.location.origin;

window.API_BASE_URL = API_BASE_URL;

console.log("API Base URL configured as:", API_BASE_URL);
