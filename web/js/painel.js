// Pequenas interacoes do painel administrativo.
(function () {
    "use strict";

    // Botao "Copiar" do link publico da barbearia
    document.querySelectorAll("[data-copiar]").forEach(function (botao) {
        botao.addEventListener("click", function () {
            var alvo = document.getElementById(botao.getAttribute("data-copiar"));
            if (!alvo) {
                return;
            }
            var texto = alvo.textContent.trim();
            var original = botao.textContent;
            var avisar = function () {
                botao.textContent = "Copiado!";
                setTimeout(function () { botao.textContent = original; }, 1800);
            };
            if (navigator.clipboard && window.isSecureContext) {
                navigator.clipboard.writeText(texto).then(avisar);
            } else {
                var area = document.createElement("textarea");
                area.value = texto;
                document.body.appendChild(area);
                area.select();
                try { document.execCommand("copy"); avisar(); } catch (e) { /* sem suporte */ }
                document.body.removeChild(area);
            }
        });
    });

    // Confirmacao generica para botoes/forms com data-confirmar
    document.querySelectorAll("form[data-confirmar]").forEach(function (form) {
        form.addEventListener("submit", function (evento) {
            if (!window.confirm(form.getAttribute("data-confirmar"))) {
                evento.preventDefault();
            }
        });
    });
})();
