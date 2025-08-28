# 📌 Guia de Execução do Projeto

Este repositório contém dois módulos principais:  
- **Servidor (Python)**  
- **Aplicativo Android (Kotlin)**  

Abaixo estão os passos para rodar cada parte do sistema.

---

## 🚀 Como rodar o **Servidor (Python)**

1. Navegue até a pasta do servidor:
   ```bash
   cd servidor
   ```

2. Instale as dependências listadas em `requirements.txt`:
   ```bash
   pip install -r requirements.txt
   ```

3. Execute o servidor:
   ```bash
   python server.py
   ```

---

## 📱 Como rodar o **Aplicativo Android (Kotlin)**

1. Navegue até a pasta do app:
   ```bash
   cd kotlin
   ```

2. Certifique-se de que o **modo desenvolvedor** e a **depuração USB** estão ativados no celular.

3. Conecte o celular via cabo USB ao notebook.

4. Execute o comando para instalar o app:
   ```bash
   ./gradlew installDebug
   ```

5. Ao final da instalação, abra o aplicativo diretamente no dispositivo Android.
