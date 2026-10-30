window.onload = function() {
  const port = window.location.port || (window.location.protocol === "https:" ? "443" : "80");
  const cookieName = "appbase_wizard_token_" + port + "=";
  const tokenCookie = document.cookie.split(";")
    .map(function(value) { return value.trim(); })
    .find(function(value) { return value.startsWith(cookieName); });
  const token = tokenCookie ? tokenCookie.substring(cookieName.length) : null;
  window.ui = SwaggerUIBundle({
    url: "/api/openapi.json",
    dom_id: '#swagger-ui',
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: "StandaloneLayout",
    requestInterceptor: function(request) {
      if (token) {
        request.headers = request.headers || {};
        request.headers["X-Appbase-Wizard-Token"] = token;
      }
      return request;
    }
  });
};
