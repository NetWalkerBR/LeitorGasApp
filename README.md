# 📱 GasLeitorApp — Cond. Ile de France

Aplicativo Android para leitura de medidores de gás dos 44 apartamentos do condomínio.

---

## 📋 Requisitos

| Item | Versão |
|------|--------|
| Android Studio | Hedgehog 2023.1.1 ou superior |
| JDK | 17 |
| Android SDK | API 34 (compilação), API 29 mínimo |
| Gradle | 8.2 |
| Android no celular | **Android 10 ou superior** |

---

## 🏗️ Como abrir e compilar

### 1. Clonar / extrair o projeto

Extraia o ZIP em uma pasta local (ex: `C:\Projetos\GasLeitorApp`).

### 2. Abrir no Android Studio

- Abra o **Android Studio**
- Clique em **"Open"** (ou File → Open)
- Selecione a pasta raiz `GasLeitorApp`
- Aguarde o Gradle sincronizar (pode demorar alguns minutos na primeira vez)

### 3. Adicionar suporte a KAPT (Room)

No arquivo `app/build.gradle`, a linha:
```groovy
annotationProcessor 'androidx.room:room-compiler:2.6.1'
```
deve ser substituída por:
```groovy
kapt 'androidx.room:room-compiler:2.6.1'
```
E adicionar no topo:
```groovy
id 'kotlin-kapt'
```

> ⚠️ Esta substituição é necessária pois o Room com Kotlin requer `kapt`, não `annotationProcessor`.

### 4. Gerar o APK

- Menu: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- O APK será gerado em: `app/build/outputs/apk/debug/app-debug.apk`

### 5. Instalar no celular

- Conecte o celular via USB com **Depuração USB** ativada
- Clique em ▶️ **Run** no Android Studio, ou
- Copie o APK para o celular e instale manualmente (permitir "fontes desconhecidas")

---

## 📱 Fluxo do aplicativo

```
[Splash Screen]
      ↓ (2,5 seg)
[Confirmar Data] → usuário confirma mês/ano
      ↓
[Tela de Leitura] → lista 44 aptos do 11º ao 1º andar
      ↓ (toca num apto)
[Painel de entrada] → digita valor → confirma
      ↓ (todos 44 lidos)
[Botão "Finalizar"] → confirmação
      ↓
[Tela de Resumo] → estatísticas + exportar Excel
```

---

## 🏢 Organização dos apartamentos

Os 44 apartamentos são listados do andar mais alto ao mais baixo:

| Andar | Apartamentos |
|-------|-------------|
| 11º   | 114, 113, 112, 111 |
| 10º   | 104, 103, 102, 101 |
| 9º    | 94, 93, 92, 91 |
| ...   | ... |
| 1º    | 14, 13, 12, 11 |

---

## 💾 Banco de dados (SQLite / Room)

As leituras ficam armazenadas localmente no dispositivo em duas tabelas:

- **`cycles`** — registra cada ciclo de leitura (mês/ano, início, fim)
- **`readings`** — armazena cada leitura individual (apto, valor, timestamp)

Os dados **persistem entre sessões** — se o zelador fechar o app no meio, as leituras já feitas são mantidas.

---

## 📊 Exportação Excel

Ao finalizar, o app gera um arquivo `.xlsx` com:
- Título e competência (mês/ano)
- Tabela com todos os 44 aptos: andar, apartamento, leitura em m³, status
- Estatísticas de completude
- Nome do arquivo: `leitura_gas_MM_AAAA.xlsx`

O arquivo fica salvo em: **Armazenamento externo do app** (acessível pelo gerenciador de arquivos) e também pode ser **compartilhado direto** via WhatsApp, e-mail, Drive, etc.

---

## 📁 Estrutura do projeto

```
GasLeitorApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/ildefrance/gasleitor/
│   │   ├── data/
│   │   │   ├── db/          # Room: Entities, DAOs, AppDatabase
│   │   │   ├── model/       # Models: Reading, ReadingCycle, ApartmentStatus
│   │   │   └── repository/  # ReadingRepository
│   │   ├── ui/
│   │   │   ├── splash/      # SplashActivity
│   │   │   ├── date/        # DateConfirmActivity
│   │   │   ├── reading/     # ReadingActivity + ApartmentAdapter
│   │   │   └── summary/     # SummaryActivity
│   │   └── util/
│   │       ├── ApartmentHelper.kt  # Gera lista dos 44 aptos
│   │       └── ExcelExporter.kt    # Exportação Apache POI
│   └── res/
│       ├── layout/          # 4 layouts de tela + 1 item de lista
│       ├── values/          # colors.xml, strings.xml, themes.xml
│       ├── drawable/        # Ícones e formas
│       └── xml/             # file_paths.xml (FileProvider)
└── app/build.gradle         # Dependências e configurações
```

---

## 🔧 Dependências principais

| Biblioteca | Finalidade |
|-----------|-----------|
| AndroidX Room 2.6 | Banco de dados SQLite local |
| Apache POI 5.2.3 | Geração de planilha Excel (.xlsx) |
| Material Components | UI moderna |
| Kotlin Coroutines | Operações assíncronas |
| ConstraintLayout / RecyclerView | Layouts |

---

## 🐛 Problemas comuns

**"Cannot find symbol: kapt"** → Adicione `id 'kotlin-kapt'` no bloco plugins do `app/build.gradle`.

**APK instalado mas não abre** → Verifique se o celular tem Android 10+.

**Excel vazio ou erro na exportação** → Certifique-se de que o celular tem espaço disponível. O arquivo fica em `Android/data/com.ildefrance.gasleitor/files/`.

---

## 📞 Suporte

Desenvolvido para uso interno do **Condomínio Ile de France**.
