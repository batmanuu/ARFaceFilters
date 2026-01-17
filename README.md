**Nome do Projeto**: AR FACE FILTERS: Aplicação de Realidade Aumentada para Filtros Faciais.
**Disciplina**: Computação Gráfica
**Turma**: 2025.2
**Nome do Aluno**: Emanuelle Victoria Fernandes Silva
**Data**: 12/01/2026

**Objetivo do projeto:** O objetivo deste projeto é desenvolver uma aplicação móvel para Android que utilize Realidade Aumentada (RA) aplicada a filtros faciais, empregando conceitos fundamentais de Computação Gráfica.
A aplicação faz uso do ARCore para rastreamento facial em tempo real e do OpenGL ES para a renderização gráfica dos filtros, permitindo a sobreposição de elementos visuais (como óculos, máscaras e textos) diretamente sobre o rosto do usuário capturado pela câmera frontal do dispositivo.

**Tecnologias Utilizadas**

**Android:** Plataforma utilizada para o desenvolvimento da aplicação móvel. O Android foi escolhido por oferecer suporte nativo ao ARCore, além de ferramentas consolidadas para desenvolvimento gráfico e acesso à câmera do dispositivo.

**ARCore (Google ARCore):** Framework de Realidade Aumentada da Google utilizado para: Acesso à câmera frontal, Rastreamento facial em tempo real, Detecção e acompanhamento de regiões do rosto (Augmented Faces). O ARCore fornece as poses e informações necessárias para posicionar corretamente os filtros sobre o rosto do usuário.

**OpenGL ES:** API gráfica utilizada para a renderização dos filtros sobre a imagem da câmera. Com o OpenGL ES foram implementados: Desenho de quads (planos 2D), Aplicação de texturas (PNG com transparência), Transformações geométricas, Composição da cena gráfica. O OpenGL ES é amplamente utilizado em aplicações gráficas em tempo real em dispositivos móveis.

**GLSurfaceView:** Classe do Android responsável por: Criar e gerenciar o contexto OpenGL, controlar o ciclo de vida da renderização, executar o loop de desenho (onDrawFrame). A GLSurfaceView é essencial para integrar o OpenGL ES ao ambiente Android.

**Transformações Gráficas (Model, View e Projection):** Foram utilizadas matrizes de transformação para posicionar corretamente os objetos gráficos na cena: Model Matrix (posiciona e escala os filtros em relação ao rosto), View Matrix (representa a posição e orientação da câmera), Projection Matrix (define a projeção da cena 3D para a tela 2D). Essas transformações são fundamentais no pipeline gráfico.

**Texturas PNG com Canal Alpha:** Os filtros (óculos, máscaras e textos) utilizam imagens no formato PNG com transparência, permitindo: Sobreposição correta sobre o rosto, Bordas suaves, Integração visual com a imagem da câmera. O canal alpha é essencial para efeitos visuais em Realidade Aumentada.

**Kotlin:** Linguagem de programação utilizada para implementar toda a lógica do aplicativo.
O Kotlin é a linguagem oficial para desenvolvimento Android e oferece: Código mais conciso, Melhor segurança contra erros, Integração direta com APIs Android e ARCore. 

**Jetpack Compose:** Framework moderno de interface utilizado para construir a tela inicial (Home) do aplicativo. Foi utilizado para: Criar a interface antes da abertura da câmera, Implementar botões e navegação entre telas

Android Studio: Ambiente de desenvolvimento integrado (IDE) utilizado para: Programação, Gerenciamento de dependências, Execução e testes da aplicação em dispositivos físicos.
Gradle: Ferramenta de automação e gerenciamento de dependências do projeto, responsável por: Configurar versões do SDK, Gerenciar bibliotecas (ARCore, Compose, OpenGL), Compilar e empacotar o aplicativo.

Arquitetura do Projeto
O projeto foi estruturado de forma modular, separando claramente as responsabilidades de interface, controle da sessão AR e renderização gráfica, facilitando a compreensão, manutenção e expansão da aplicação.
A arquitetura é composta principalmente pelas seguintes camadas:
MainActivity
Responsável pela tela inicial (Home) do aplicativo.
Funções principais: Exibir a interface inicial antes da abertura da câmera; Utilizar Jetpack Compose para construção da UI; Fornecer um botão que inicia a experiência de Realidade Aumentada; Realizar a navegação para a ARFaceActivity.
A separação da Home evita que a câmera seja aberta imediatamente, tornando o aplicativo mais organizado e apresentável.

ARFaceActivity
Activity responsável pela execução da Realidade Aumentada.
Funções principais: Inicializar a sessão do ARCore utilizando a câmera frontal; Configurar o modo Augmented Faces; Gerenciar permissões de câmera; Controlar o ciclo de vida da sessão AR (resume/pause); Integrar a GLSurfaceView com o loop de renderização (Renderer)
Essa classe atua como o controlador central da aplicação de RA.

GLSurfaceView
Componente responsável por: Criar e manter o contexto OpenGL ES; Executar o loop de renderização contínuo; Chamar os métodos: onSurfaceCreated, onSurfaceChanged, onDrawFrame.
A GLSurfaceView permite integrar a renderização gráfica em tempo real ao ambiente Android.

BackgroundRenderer
Classe responsável por renderizar a imagem da câmera como fundo da cena.
Funções principais: Criar uma textura externa (GL_TEXTURE_EXTERNAL_OES); Receber os frames da câmera via ARCore; Ajustar corretamente a imagem conforme a rotação do dispositivo; Desenhar o fundo da cena antes dos filtros.
Essa etapa é essencial para compor a cena de Realidade Aumentada.

GlassesRenderer (Renderer de Overlays)
Classe responsável pela renderização dos filtros faciais.
Funções principais: Renderizar quads (planos 2D) sobre o rosto; Aplicar texturas PNG com transparência (óculos, texto, máscaras); Controlar posição, escala e profundidade dos objetos; Realizar o blend correto utilizando canal alpha; Ancorar os objetos em regiões específicas do rosto (nariz, testa, etc.)
Essa classe é reutilizada para diferentes filtros, variando apenas os parâmetros de transformação e a textura aplicada.

Sistema de Transformações
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




