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

Segundo botão redondo no painel (**S**), com calibração própria (**⚙ calibrate S**). Todos os toques do S saem como **um gesto só, com cada toque numa linha do tempo**; o sistema dispara no tempo certo, sem atraso entre um passo e outro. Por isso os tempos são **do começo de um toque ao começo do próximo** (igual ao macro de PC: `3` → 10 ms → `SPACE`), e os toques podem se sobrepor.

Sequência e tempos padrão:

1. **swipe da câmera pra cima** (pro chute ir pra cima)
2. **M1** segurado por **50 ms** (chute médio) → 40 ms depois de soltar
3. **slot 3** → **+10 ms** → **pulo** (se o 3 sair na hora errada em relação ao pulo, o jogo usa a variação do 3)
4. **+15 ms** → **modo técnica**
5. **+25 ms** (a cabeçada acontecer) → **slot 3** de novo
6. **+35 ms** → **slot 2 + T ao mesmo tempo**

Calibração do S, na ordem: início do swipe, fim do swipe, M1, slot 3, pulo, modo técnica, slot 2, T.

Todos os tempos ficam no card *Shidou macro* do app. Duração do toque, do swipe, delay após o swipe e lockout vêm do card *Timings*.

## Notas

- Calibre na mesma orientação em que vai jogar. Se mudar de orientação, o app avisa pra recalibrar.
- Se mudar o layout da hotbar ou o tamanho da UI do Roblox, recalibre.
- iPhone não tem equivalente sem jailbreak.
