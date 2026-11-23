// Janela do assistente com IA: envia a mensagem para o servidor (/chat)
// e mostra a resposta. Toda a conversa com a IA acontece no servidor.
(function () {
    "use strict";

    var chat = document.getElementById("chat");
    if (!chat) {
        return;
    }
    var lista = chat.querySelector(".chat-mensagens");
    var form = chat.querySelector(".chat-form");
    var campo = chat.querySelector("#chat-texto");
    var botaoEnviar = form.querySelector("button[type=submit]");
    var flutuante = document.querySelector(".chat-botao-flutuante");
    var ocupado = false;

    function abrir() {
        chat.hidden = false;
        document.body.classList.add("chat-aberto");
        if (flutuante) {
            flutuante.hidden = true;
        }
        setTimeout(function () { campo.focus(); }, 50);
    }

    function fechar() {
        chat.hidden = true;
        document.body.classList.remove("chat-aberto");
        if (flutuante) {
            flutuante.hidden = false;
        }
    }

    // Texto simples -> HTML seguro (escapa tudo, depois aplica **negrito** e quebras de linha)
    function formatar(texto) {
        var div = document.createElement("div");
        div.textContent = texto;
        return div.innerHTML
            .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
            .replace(/\n/g, "<br>");
    }

    function adicionar(texto, quem) {
        var msg = document.createElement("div");
        msg.className = "chat-msg chat-msg-" + quem;
        msg.innerHTML = formatar(texto);
        lista.appendChild(msg);
        lista.scrollTop = lista.scrollHeight;
        return msg;
    }

    function digitando() {
        var msg = document.createElement("div");
        msg.className = "chat-msg chat-msg-ia chat-digitando";
        msg.setAttribute("aria-label", "O assistente está digitando");
        msg.innerHTML = "<span></span><span></span><span></span>";
        lista.appendChild(msg);
        lista.scrollTop = lista.scrollHeight;
        return msg;
    }

    function enviar(texto, extra) {
        if (ocupado) {
            return;
        }
        ocupado = true;
        botaoEnviar.disabled = true;
        var sugestoes = lista.querySelector(".chat-sugestoes");
        if (sugestoes) {
            sugestoes.remove();
        }
        if (texto) {
            adicionar(texto, "cliente");
        }
        var indicador = digitando();

        var corpo = new URLSearchParams();
        corpo.append("b", chat.dataset.barbearia);
        if (texto) {
            corpo.append("mensagem", texto);
        }
        if (extra) {
            Object.keys(extra).forEach(function (k) { corpo.append(k, extra[k]); });
        }

        fetch(chat.dataset.url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: corpo
        }).then(function (r) {
            return r.json();
        }).then(function (dados) {
            indicador.remove();
            if (dados.resposta) {
                var msg = adicionar(dados.resposta, dados.ok ? "ia" : "aviso");
                if (dados.agendamento) {
                    var link = document.createElement("a");
                    link.className = "btn btn-secundario btn-pequeno chat-link";
                    link.href = dados.agendamento;
                    link.textContent = "Ver agendamento";
                    msg.appendChild(document.createElement("br"));
                    msg.appendChild(link);
                }
            }
        }).catch(function () {
            indicador.remove();
            adicionar("Não consegui falar com o assistente agora. Tente de novo em instantes ou agende pela página.", "aviso");
        }).then(function () {
            ocupado = false;
            botaoEnviar.disabled = false;
            campo.focus();
        });
    }

    document.querySelectorAll("[data-abrir-chat]").forEach(function (b) { b.addEventListener("click", abrir); });
    chat.querySelector("[data-fechar-chat]").addEventListener("click", fechar);
    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape" && !chat.hidden) {
            fechar();
        }
    });

    chat.querySelector("[data-nova-conversa]").addEventListener("click", function () {
        if (ocupado) {
            return;
        }
        lista.innerHTML = "";
        adicionar("Conversa nova! Como posso ajudar?", "ia");
        fetch(chat.dataset.url, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({ b: chat.dataset.barbearia, reiniciar: "1" })
        });
    });

    lista.addEventListener("click", function (e) {
        if (e.target.matches("[data-sugestao]")) {
            enviar(e.target.textContent.trim());
        }
    });

    form.addEventListener("submit", function (e) {
        e.preventDefault();
        var texto = campo.value.trim();
        if (!texto) {
            return;
        }
        campo.value = "";
        enviar(texto);
    });

    if (chat.dataset.abrir === "true") {
        abrir();
    }
})();
