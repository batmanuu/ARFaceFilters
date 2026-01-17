**Nome do Projeto**: AR FACE FILTERS: Aplicação de Realidade Aumentada para Filtros Faciais.

**Disciplina**: Computação Gráfica

**Turma**: 2025.2

**Nome do Aluno**: Emanuelle Victoria Fernandes Silva

**Data**: 12/01/2026

**Objetivo do projeto:** O objetivo deste projeto é desenvolver uma aplicação móvel para Android que utilize Realidade Aumentada (RA) aplicada a filtros faciais, empregando conceitos fundamentais de Computação Gráfica.
A aplicação faz uso do ARCore para rastreamento facial em tempo real e do OpenGL ES para a renderização gráfica dos filtros, permitindo a sobreposição de elementos visuais (como óculos, máscaras e textos) diretamente sobre o rosto do usuário capturado pela câmera frontal do dispositivo.

***Tecnologias Utilizadas***

**Android:** Plataforma utilizada para o desenvolvimento da aplicação móvel. O Android foi escolhido por oferecer suporte nativo ao ARCore, além de ferramentas consolidadas para desenvolvimento gráfico e acesso à câmera do dispositivo.

**ARCore (Google ARCore):** Framework de Realidade Aumentada da Google utilizado para: Acesso à câmera frontal, Rastreamento facial em tempo real, Detecção e acompanhamento de regiões do rosto (Augmented Faces). O ARCore fornece as poses e informações necessárias para posicionar corretamente os filtros sobre o rosto do usuário.

**OpenGL ES:** API gráfica utilizada para a renderização dos filtros sobre a imagem da câmera. Com o OpenGL ES foram implementados: Desenho de quads (planos 2D), Aplicação de texturas (PNG com transparência), Transformações geométricas, Composição da cena gráfica. O OpenGL ES é amplamente utilizado em aplicações gráficas em tempo real em dispositivos móveis.

**GLSurfaceView:** Classe do Android responsável por: Criar e gerenciar o contexto OpenGL, controlar o ciclo de vida da renderização, executar o loop de desenho (onDrawFrame). A GLSurfaceView é essencial para integrar o OpenGL ES ao ambiente Android.

**Transformações Gráficas (Model, View e Projection):** Foram utilizadas matrizes de transformação para posicionar corretamente os objetos gráficos na cena: Model Matrix (posiciona e escala os filtros em relação ao rosto), View Matrix (representa a posição e orientação da câmera), Projection Matrix (define a projeção da cena 3D para a tela 2D). Essas transformações são fundamentais no pipeline gráfico.

**Texturas PNG com Canal Alpha:** Os filtros (óculos, máscaras e textos) utilizam imagens no formato PNG com transparência, permitindo: Sobreposição correta sobre o rosto, Bordas suaves, Integração visual com a imagem da câmera. O canal alpha é essencial para efeitos visuais em Realidade Aumentada.

**Kotlin:** Linguagem de programação utilizada para implementar toda a lógica do aplicativo.
O Kotlin é a linguagem oficial para desenvolvimento Android e oferece: Código mais conciso, Melhor segurança contra erros, Integração direta com APIs Android e ARCore. 

**Jetpack Compose:** Framework moderno de interface utilizado para construir a tela inicial (Home) do aplicativo. Foi utilizado para: Criar a interface antes da abertura da câmera, Implementar botões e navegação entre telas

**Android Studio:** Ambiente de desenvolvimento integrado (IDE) utilizado para: Programação, Gerenciamento de dependências, Execução e testes da aplicação em dispositivos físicos.

**Gradle:** Ferramenta de automação e gerenciamento de dependências do projeto, responsável por: Configurar versões do SDK, Gerenciar bibliotecas (ARCore, Compose, OpenGL), Compilar e empacotar o aplicativo.


***Arquitetura do Projeto***


O projeto foi estruturado de forma modular, separando claramente as responsabilidades de interface, controle da sessão AR e renderização gráfica, facilitando a compreensão, manutenção e expansão da aplicação.
A arquitetura é composta principalmente pelas seguintes camadas:

**MainActivity**
Responsável pela tela inicial (Home) do aplicativo.
Funções principais: Exibir a interface inicial antes da abertura da câmera; Utilizar Jetpack Compose para construção da UI; Fornecer um botão que inicia a experiência de Realidade Aumentada; Realizar a navegação para a ARFaceActivity.
A separação da Home evita que a câmera seja aberta imediatamente, tornando o aplicativo mais organizado e apresentável.

**ARFaceActivity**
Activity responsável pela execução da Realidade Aumentada.
Funções principais: Inicializar a sessão do ARCore utilizando a câmera frontal; Configurar o modo Augmented Faces; Gerenciar permissões de câmera; Controlar o ciclo de vida da sessão AR (resume/pause); Integrar a GLSurfaceView com o loop de renderização (Renderer)
Essa classe atua como o controlador central da aplicação de RA.

**GLSurfaceView**
Componente responsável por: Criar e manter o contexto OpenGL ES; Executar o loop de renderização contínuo; Chamar os métodos: onSurfaceCreated, onSurfaceChanged, onDrawFrame.
A GLSurfaceView permite integrar a renderização gráfica em tempo real ao ambiente Android.

**BackgroundRenderer**
Classe responsável por renderizar a imagem da câmera como fundo da cena.
Funções principais: Criar uma textura externa (GL_TEXTURE_EXTERNAL_OES); Receber os frames da câmera via ARCore; Ajustar corretamente a imagem conforme a rotação do dispositivo; Desenhar o fundo da cena antes dos filtros.
Essa etapa é essencial para compor a cena de Realidade Aumentada.

**GlassesRenderer (Renderer de Overlays)**
Classe responsável pela renderização dos filtros faciais.
Funções principais: Renderizar quads (planos 2D) sobre o rosto; Aplicar texturas PNG com transparência (óculos, texto, máscaras); Controlar posição, escala e profundidade dos objetos; Realizar o blend correto utilizando canal alpha; Ancorar os objetos em regiões específicas do rosto (nariz, testa, etc.)
Essa classe é reutilizada para diferentes filtros, variando apenas os parâmetros de transformação e a textura aplicada.

**Sistema de Transformações**
O posicionamento correto dos filtros é feito através de transformações geométricas, aplicadas em sequência:
1.	Model Matrix
o	Define a posição e escala do objeto em relação ao rosto
o	Utiliza a pose fornecida pelo ARCore
o	Aplica deslocamentos manuais (offsets)
2.	View Matrix
o	Representa a posição e orientação da câmera virtual
o	Obtida diretamente do ARCore
3.	Projection Matrix
o	Define como a cena 3D é projetada na tela 2D
o	Considera perspectiva e profundidade


***Pipeline Gráfico da Aplicação***

O pipeline gráfico da aplicação descreve o conjunto de etapas responsáveis por transformar os dados capturados pela câmera em uma cena final renderizada na tela, combinando imagem real e elementos virtuais em tempo real.
Neste projeto, o pipeline gráfico segue as etapas descritas a seguir.

**Captura da Imagem da Câmera (ARCore):** A aplicação utiliza o ARCore para acessar a câmera frontal do dispositivo. Cada frame capturado contém:
•	A imagem da câmera
•	Informações de rastreamento do rosto
•	Poses e regiões faciais (Augmented Faces)
O ARCore fornece esses dados já sincronizados, permitindo a renderização contínua da cena.

**Textura da Câmera como Fundo da Cena:** A imagem da câmera é enviada para o OpenGL ES por meio de uma textura externa (GL_TEXTURE_EXTERNAL_OES).
A classe BackgroundRenderer é responsável por: Criar essa textura externa, Receber os frames da câmera, Ajustar corretamente a imagem conforme a rotação do dispositivo,. Desenhar a câmera como plano de fundo da cena.
Essa etapa garante que a cena virtual seja composta sobre a imagem real.

**Rastreamento Facial (Augmented Faces):** O ARCore detecta e acompanha o rosto do usuário em tempo real, fornecendo: Posição e orientação do rosto, Regiões específicas (nariz, testa, etc.), Atualizações contínuas enquanto o rosto está visível.
Essas informações são utilizadas como âncoras para posicionar corretamente os filtros gráficos.

**Construção da Model Matrix:** A Model Matrix define a posição, escala e orientação de cada objeto virtual em relação ao rosto.
Nesta aplicação, a Model Matrix é composta por: A pose da região facial fornecida pelo ARCore, Translações manuais (offsetX, offsetY, offsetZ), Escalas manuais (sizeY, wide)
Esses ajustes permitem alinhar visualmente os filtros ao rosto do usuário.

**View Matrix (Câmera Virtual):** A View Matrix representa a posição e orientação da câmera virtual na cena. Ela é obtida diretamente do ARCore e corresponde ao ponto de vista da câmera real do dispositivo. Essa matriz garante que os objetos virtuais sejam renderizados com a perspectiva correta em relação à câmera.

**Projection Matrix (Projeção da Cena)**
A Projection Matrix define como a cena tridimensional será projetada na tela bidimensional.
A projeção utilizada considera: Campo de visão da câmera, Profundidade (near e far planes), Perspectiva realista dos objetos. Essa matriz é essencial para manter a coerência visual entre os elementos virtuais e a imagem real.
_____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________



***Filtros implementados***

**ÓCULOS + TEXTO**


![WhatsApp Image 2026-01-17 at 19 32 19](https://github.com/user-attachments/assets/df30b281-670f-4dff-a23f-61ad3adca066)


Óculos pixelados (“deal with it”)
Renderizados como uma textura PNG com transparência, posicionados dinamicamente sobre a região dos olhos. O tamanho e a posição são ajustados a partir da largura do rosto detectada pelo ARCore, garantindo alinhamento mesmo com movimentos da cabeça.

Texto curvo “COMPUTAÇÃO GRÁFICA”
Um elemento textual em formato de arco, exibido acima da cabeça. O texto acompanha a rotação e a translação do rosto, mantendo coerência espacial (ancoragem facial) e reforçando o tema acadêmico do projeto.

Interação em tempo real
Ambos os elementos reagem instantaneamente a movimentos, rotações e mudanças de distância da câmera, demonstrando rastreamento facial contínuo.

Conceitos técnicos aplicados:

ARCore Augmented Faces para detecção e rastreamento facial.

OpenGL ES 2.0 para renderização de quads texturizados.

Transformações geométricas (model, view, projection) para posicionamento correto no espaço 3D.

Blending com canal alpha para integrar PNGs ao feed da câmera sem recortes artificiais.

Objetivo do filtro: Demonstrar, de forma visual e interativa, como técnicas de Computação Gráfica podem ser aplicadas em dispositivos móveis para criar experiências de realidade aumentada estáveis, expressivas e responsivas.

_____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

**MÁSCARA DO BATMAN**

![WhatsApp Image 2026-01-17 at 19 32 26](https://github.com/user-attachments/assets/9c4f0f0d-5b7e-48c1-bf8a-0e51327f34cf)

Este filtro aplica uma máscara facial completa em tempo real utilizando ARCore Augmented Faces. A máscara é renderizada como uma textura PNG com transparência, posicionada diretamente sobre o rosto do usuário, acompanhando com precisão os movimentos da cabeça e as expressões faciais.

Funcionamento técnico:

Utiliza detecção facial em tempo real com a câmera frontal.

A máscara é ancorada ao NOSE_TIP e ajustada manualmente nos eixos Y (altura) e Z (profundidade) para garantir alinhamento correto.

O tamanho do filtro é escalado dinamicamente com base na largura do rosto, calculada a partir dos pontos FOREHEAD_LEFT e FOREHEAD_RIGHT.

Renderização feita via OpenGL ES 2.0, com blending ativado para respeitar a transparência da textura.

O filtro permanece estável mesmo com rotação e inclinação do rosto.


Objetivo do filtro:

Demonstrar o uso de:

Mapeamento facial em Realidade Aumentada

Transformações geométricas (translação e escala)

Renderização 2D aplicada sobre um modelo facial 3D

Esse filtro exemplifica a integração entre Computação Gráfica e Realidade Aumentada, aplicando conceitos práticos de visualização gráfica em dispositivos móveis Android.

_____________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

**MÁSCARA DO HOMEM ARANHA**

![WhatsApp Image 2026-01-17 at 19 32 34](https://github.com/user-attachments/assets/723418c0-8d89-490b-bff0-5b7e84fc8292)
![WhatsApp Image 2026-01-17 at 19 32 48](https://github.com/user-attachments/assets/2a9b8c16-f0ba-4173-97ca-ecaae0e8b05f)
![WhatsApp Image 2026-01-17 at 19 32 39](https://github.com/user-attachments/assets/db7bb9a5-b2c7-4883-8eca-3a3dccad21d9)


Este filtro simula uma pintura aplicada diretamente sobre o rosto, acompanhando com precisão os movimentos da cabeça e as expressões faciais em tempo real. Diferente de acessórios rígidos (como óculos ou máscaras físicas), o efeito é orgânico, dando a impressão de que o desenho faz parte da pele.

Características principais:

Mapeamento facial em tempo real: a textura é ancorada ao rosto usando Augmented Faces do ARCore, mantendo alinhamento correto mesmo com rotações e inclinações.

Efeito de pintura: a imagem é aplicada como overlay plano sobre a face, com transparência (PNG com alpha), criando o visual de pintura.

Desempenho em tempo real: renderização via OpenGL ES, garantindo resposta imediata aos movimentos.

Uso no aplicativo:

O filtro pode ser alternado pelo botão “Trocar Filtro”.

Ideal para efeitos artísticos, temáticos ou demonstrações de Computação Gráfica aplicada à Realidade Aumentada.

Este filtro evidencia conceitos de CG como texturização, transformações geométricas, blending (alpha) e rastreamento facial, integrados em um aplicativo Android com ARCore.
