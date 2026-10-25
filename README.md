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

Segundo botão redondo no painel (**S**). Ele **reaproveita a calibração do macro 3** pro swipe da câmera, chute (M1), slot 3 e pulo (mesmos pontos da tela, mesma duração de toque, de swipe e mesmo delay após o swipe). O **⚙ calibrate S** só pede 3 pontos, na ordem: **modo técnica, slot 2, T**. Calibre o 3 antes.

Sequência (mesma mecânica do macro 3: um toque de cada vez, e o delay conta do **fim** de um toque até o começo do próximo):

1. **swipe da câmera pra cima**
2. **M1** segurado por **50 ms** → **150 ms** de espera (tempo pro chute sair, igual ao *Delay after kick* do macro 3)
3. **slot 3** → **10 ms** → **pulo** (igual ao macro 3)
4. **modo técnica** (0 ms depois do pulo)
5. **25 ms** (a cabeçada acontecer) → **slot 3** de novo
6. **35 ms** → **slot 2 + T ao mesmo tempo** (dois dedos no mesmo gesto)

Os tempos do S ficam no card *Shidou macro* do app. Se a primeira parte (chute, 3 e pulo) não sair igual ao macro 3, compare *Hold M1* (50 ms) com *Tap duration* (30 ms) e *Delay M1 → slot 3* (150 ms) com *Delay after kick* (150 ms).

## Usar andando (joystick segurado)

O painel usa *split touch*, então ele recebe o toque do segundo dedo mesmo com o primeiro dedo segurando o joystick. Se um passo do macro for cancelado pelo Android (acontece quando há um dedo de verdade na tela), aparece o aviso *Gesture cancelled by the system*.

## Notas

- Calibre na mesma orientação em que vai jogar. Se mudar de orientação, o app avisa pra recalibrar.
- Se mudar o layout da hotbar ou o tamanho da UI do Roblox, recalibre.
- iPhone não tem equivalente sem jailbreak.

## Modo teclas (anda sem travar)

Nova opção na tela do app: **Keys mode for macro 3**. Em vez de tocar na tela, o macro 3 manda as teclas **3 → ESPAÇO → 1** como teclado pelo Shizuku. Como não injeta nenhum toque, o dedo no joystick não é derrubado.

- Precisa do Shizuku rodando e conectado (**Connect Shizuku**).
- Swipe da câmera e chute (que são toque) ficam de fora nesse modo, e o macro 3 não precisa de calibração.
- O macro S continua usando toque.
- Se nada acontecer no jogo, o Roblox/Delta pode não estar aceitando teclado injetado: aí avisa que a gente ajusta.

## Script mode (Delta)

The "3" button can call a Lua script inside Delta instead of tapping the screen. Turn on **Use script for macro 3** in the app, then run `delta/violet_bridge.lua` in Delta. The app listens on 127.0.0.1:38417 and the script runs camera up, kick, slot 3, jump and slot 1 inside the game. For the kick, paste the script below the part of your script that defines `buffers`.
