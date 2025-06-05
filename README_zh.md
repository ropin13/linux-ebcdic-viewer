# EBCDIC 文件查看器

## 概述
EBCDIC 文件查看器是一款基于 Java 的命令行工具，旨在以人类可读的格式显示 EBCDIC (扩展二进制编码十进制交换码) 编码的数据文件。它使用 COBOL copybook 来解析 EBCDIC 文件中的记录结构，允许用户查看字段名及其对应的值。该工具提供了一个基于文本的用户界面 (TUI) 用于导航和基本的数据交互。

对于需要检查大型机数据文件或其他 EBCDIC 编码数据源但又缺乏专门的大型机访问权限或工具的开发人员和数据分析师来说，此工具尤其有用。

## 功能特性
- **EBCDIC 数据解析**: 读取并解析 EBCDIC 编码文件。
- **基于 Copybook 的字段定义**: 使用 COBOL copybook 定义数据文件中记录的结构。
- **文本用户界面 (TUI)**: 提供交互式终端界面来查看数据。
- **分頁瀏覽 (Paged Data Browsing)**: 将数据分页显示，便于管理和查看大型文件。
    - 导航到下一页/上一页。
- **欄位搜尋 (Field-based Search)**: 允许在当前页的指定字段内搜索特定文本。
- **自定义编码**: 支持指定不同的 EBCDIC 编码。
- **自定义页面大小**: 允许用户定义每页显示的记录数。
- **控制字符净化**: 替换不可打印的 EBCDIC 控制字符，以防止显示问题。

## 先决条件
- **Java Development Kit (JDK)**: 版本 11 或更高。
- **Lanterna 3.1.1 库**: 需要 `lanterna-3.1.1.jar` 文件。它应放置在项目根目录的 `lib` 文件夹中。`compile.sh` 脚本会检查此文件。如果缺失，您通常可以从 Maven Central 或其他代码仓库下载 (搜索 "lanterna-3.1.1.jar")。

## 目录结构
```
.
├── lib/
│   └── lanterna-3.1.1.jar  # Lanterna 库
├── src/
│   ├── AppController.java
│   ├── CopybookLoader.java
│   ├── EbcdicFileViewer.java # 主要入口点
│   ├── PagedFileReader.java
│   ├── SearchManager.java
│   └── TUIView.java
├── out/                      # 编译后 .class 文件的默认目录 (由 compile.sh 创建)
├── data/                     # 可选: 用于存储数据/copybook 文件。
│                             # AppController.main() 在无参数运行时会在此创建示例文件。
├── README.md                 # 英文版 README
├── README_zh.md              # 本文件 (中文版 README)
├── compile.sh                # 编译脚本
└── run.sh                    # 运行脚本
```

## 编译
项目包含一个 `compile.sh` 脚本以简化编译过程。请确保它具有执行权限 (`chmod +x compile.sh`)。

编译命令:
```bash
bash compile.sh
```
此脚本会将编译后的 `.class` 文件放置在 `out/` 目录中。

或者，您可以手动编译 (请确保 `out/` 目录已存在):
```bash
javac -cp "lib/lanterna-3.1.1.jar:src" src/*.java -d out
```

## 执行
项目包含一个 `run.sh` 脚本以便于执行。请确保它具有执行权限 (`chmod +x run.sh`)。

运行应用程序:
```bash
bash run.sh <data_file_path> <copybook_file_path> [encoding] [page_size]
```
如果缺少参数，请遵循脚本打印的使用说明。

或者，编译后您可以手动运行:
```bash
java -cp "lib/lanterna-3.1.1.jar:out" EbcdicFileViewer <data_file_path> <copybook_file_path> [encoding] [page_size]
```
(如果您的类文件放置在包中，例如 `com.viewer`，则命令应为 `java -cp "lib/lanterna-3.1.1.jar:out" com.viewer.EbcdicFileViewer ...`)

### 命令行参数
-   `<data_file_path>`: EBCDIC 数据文件的路径 (必需)。
-   `<copybook_file_path>`: COBOL copybook 文件的路径 (必需)。
-   `[encoding]`: 要使用的 EBCDIC 编码 (可选, 默认为 "IBM037")。常见示例: `CP037`, `IBM500`, `IBM1047`。
-   `[page_size]`: 每页的记录数 (可选, 默认为 50)。必须是正整数。

## 示例用法 (使用内部生成的示例数据)
`AppController` 类包含一个 `main` 方法，如果在没有参数的情况下运行，它可以生成并使用示例 EBCDIC 数据和示例 copybook。这对于快速测试应用程序的 UI 和核心功能非常有用，无需外部文件。

使用示例数据运行 (编译后):
```bash
java -cp "lib/lanterna-3.1.1.jar:out" AppController
```
如果 `data/test.dat` 和 `data/test.cpy` 文件不存在，此命令将会创建它们，并使用这些数据启动查看器。

## 性能说明
- **内存**: 应用程序一次将一页数据加载到内存中。"页面" 的大小由每页的记录数 (用户定义) 和每条记录的长度 (由 copybook 定义) 决定。对于非常大的记录长度或非常高的页面大小，内存使用量可能会增加。
- **速度**:
    - 文件读取使用 `RandomAccessFile` 以高效地定位到页面边界。
    - 对于显示页面上的每条记录的每个字段，都会进行 EBCDIC 到字符串的转换和后续的净化处理。
    - 搜索操作仅在当前加载的页面数据上执行，并且不区分大小写。
- **大文件**: 该工具通过逐页处理数据的方式来处理大文件。初始计算总记录数需要读取文件大小，这通常很快。
- **TUI 渲染**: Lanterna 通常在 TUI 渲染方面效率较高。性能可能因终端模拟器和系统环境而异。
```
