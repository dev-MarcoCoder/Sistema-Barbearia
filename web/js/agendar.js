// Melhorias de usabilidade da pagina de agendamento.
// A pagina funciona sem JavaScript; este arquivo so deixa o fluxo mais rapido.
(function () {
    "use strict";

    // 1) Ao escolher servico, barbeiro ou dia, recarrega a pagina ja na proxima pergunta.
    var proximaEtapa = { servico: "etapa-barbeiro", barbeiro: "etapa-data", data: "horarios" };
    var form = document.querySelector("[data-auto-submit]");
    if (form) {
        form.addEventListener("change", function (evento) {
            var destino = proximaEtapa[evento.target.name] || "";
            form.action = "agendar.jsp" + (destino ? "#" + destino : "");
            form.submit();
        });
    }

    // 2) Mostra o horario escolhido no resumo e leva o cliente aos campos de dados
    document.querySelectorAll("input[name=hora]").forEach(function (radio) {
        radio.addEventListener("change", function () {
            var alvo = document.getElementById("resumo-hora");
            if (alvo) {
                alvo.textContent = radio.value;
            }
            var dados = document.querySelector(".dados-cliente");
            if (dados && dados.getBoundingClientRect().top > window.innerHeight - 120) {
                dados.scrollIntoView({ behavior: "smooth", block: "start" });
            }
        });
    });

    // 3) Mascara de telefone: (11) 98765-4321
    document.querySelectorAll("input[type=tel]").forEach(function (campo) {
        campo.addEventListener("input", function () {
            var d = campo.value.replace(/\D/g, "").slice(0, 11);
            var meio = d.length === 11 ? 7 : 6;
            var texto = d;
            if (d.length > 2) {
                texto = "(" + d.slice(0, 2) + ") " + d.slice(2);
            }
            if (d.length > meio) {
                texto = "(" + d.slice(0, 2) + ") " + d.slice(2, meio) + "-" + d.slice(meio);
            }
            campo.value = texto;
        });
    });

    // 4) Evita clique duplo no botao de confirmar
    document.querySelectorAll("form[method=post]").forEach(function (f) {
        f.addEventListener("submit", function () {
            var botao = f.querySelector("button[type=submit]");
            if (botao) {
                setTimeout(function () { botao.disabled = true; }, 0);
            }
        });
    });
})();
