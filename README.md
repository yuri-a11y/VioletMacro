# Violet Macro — Android

Porte do macro de PC (`3 → SPACE + 1`) pro celular. No Android não dá pra "ouvir" o toque no slot 3 de dentro do Roblox, então o gatilho vira um **botão flutuante**: você toca nele e o app faz os toques por você (slot 3 → pulo → slot 1), com um switch ON/OFF no mesmo painel.

Funciona por cima do Roblox / Delta (é só um app de acessibilidade tocando na tela, não mexe no jogo).

## Gerar o APK

**Opção A — sem PC (GitHub Actions)**
1. Crie um repositório no GitHub e suba o conteúdo desta pasta (descompactada).
2. Aba **Actions → build-apk → Run workflow**.
3. Quando terminar, baixe o artefato **VioletMacro-debug-apk** (vem um zip com `app-debug.apk`).

**Opção B — Android Studio (PC)**
1. `File → Open` nesta pasta, espere o Gradle sincronizar.
2. `Build → Build APK(s)`. O arquivo sai em `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar e ligar

1. Instale o APK (permita "instalar apps desconhecidos" pro navegador/gerenciador de arquivos).
2. Abra o **Violet Macro** → **Ativar serviço de acessibilidade** → ligue o serviço.
   - Android 13+: se o serviço aparecer cinza/bloqueado, vá em *Configurações → Apps → Violet Macro → ⋮ → Permitir configurações restritas* e tente de novo.
3. Abra o Roblox (Delta) **em paisagem**. O painel violeta aparece por cima.

## Usar

1. No painel, toque em **⚙ calibrar** e toque, na ordem pedida: **onde o swipe da câmera começa**, **onde termina**, **botão de chute**, **slot 3**, **botão de pulo**, **slot 1**. Salva sozinho.
2. Ligue o switch **ON**.
3. Tocar no botão redondo **3** do painel dispara a sequência. Arraste o painel pelos três pontinhos.

## Ajustes (no app)

Tempos padrão: toque 30 ms, espera após o 3 de 10 ms, pulo → 1 de 0 ms, bloqueio de 250 ms. Se o jogo "comer" algum toque, aumente a duração do toque (40–60 ms). Se você prefere tocar no slot 3 com o dedo e só quer pulo + slot 1 no botão, desligue **Botão também toca no slot 3** e recalibre.

## Macro S (Shidou)

Segundo botão redondo no painel (**S**), com calibração própria (**⚙ calibrate S**). Sequência:

1. segura o **M1** (padrão 500 ms, ajustável no app em *Shidou macro → Hold M1*)
2. **pulo**
3. **slot 3**
4. **modo técnica**
5. **slot 3** de novo
6. **slot 2 + T ao mesmo tempo** (dois dedos no mesmo gesto)

Calibração do S, na ordem: M1, pulo, slot 3, modo técnica, slot 2, T.

Ajustes: *Hold M1* (100–2000 ms) e *Gap between steps* (0–300 ms, pausa entre cada passo). A duração do toque e o lockout vêm do card *Timings*. Se o jogo comer o 3 logo depois do pulo, aumente o gap.

## Notas

- Calibre na mesma orientação em que vai jogar. Se mudar de orientação, o app avisa pra recalibrar.
- Se mudar o layout da hotbar ou o tamanho da UI do Roblox, recalibre.
- iPhone não tem equivalente sem jailbreak.
