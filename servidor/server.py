import socket
import struct
import cv2
import numpy as np
import os
import datetime
import tkinter as tk
from PIL import Image, ImageTk


def criar_diretorio_data():
    if not os.path.exists("data"):
        os.makedirs("data")
    return "data"

def caminho_imagem():
    hoje = datetime.date.today().strftime("%Y-%m-%d")
    agora = datetime.datetime.now().strftime("%H%M%S")
    dir_data = os.path.join("data", hoje)
    os.makedirs(dir_data, exist_ok=True)
    return os.path.join(dir_data, f"{agora}.jpg"), f"{agora}.jpg"


class PolaroidWindow:
    def __init__(self, titulo="Polaroid Camera"):
        self.root = tk.Tk()
        self.root.title(titulo)
        self.label = tk.Label(self.root)
        self.label.pack(expand=True, fill="both")
        self.root.geometry("800x600")
        self.root.resizable(True, True)

    def mostrar(self, img_cv2, legenda="Imagem"):
        if img_cv2 is None:
            print("Imagem inválida!")
            return

        img_rgb = cv2.cvtColor(img_cv2, cv2.COLOR_BGR2RGB)

        def atualizar_imagem(event):
            largura_nova = event.width
            altura_nova = event.height

            escala_w = largura_nova / img_rgb.shape[1]
            escala_h = altura_nova / img_rgb.shape[0]
            escala = min(escala_w, escala_h)

            nova_largura = max(1, int(img_rgb.shape[1] * escala))
            nova_altura = max(1, int(img_rgb.shape[0] * escala))

            img_resized = cv2.resize(img_rgb, (nova_largura, nova_altura))

            borda_baixo = 80
            canvas = np.ones((nova_altura + borda_baixo, nova_largura, 3), dtype=np.uint8) * 255
            canvas[:nova_altura, :, :] = img_resized

            # Legenda centralizada
            fonte = cv2.FONT_HERSHEY_SIMPLEX
            escala_fonte = max(1, nova_largura // 300)
            espessura = max(1, escala_fonte)
            (text_largura, text_altura), _ = cv2.getTextSize(legenda, fonte, escala_fonte, espessura)
            x = (nova_largura - text_largura) // 2
            y = nova_altura + borda_baixo - 10
            cv2.putText(canvas, legenda, (x, y), fonte, escala_fonte, (0,0,0), espessura, cv2.LINE_AA)

            img_pil = Image.fromarray(canvas)
            img_tk = ImageTk.PhotoImage(img_pil)

            self.label.imgtk = img_tk
            self.label.config(image=img_tk)

        self.label.bind("<Configure>", atualizar_imagem)
        self.root.update()
        atualizar_imagem(self.label)


def receber_tudo(sock, n):
    dados = b""
    while len(dados) < n:
        pacote = sock.recv(n - len(dados))
        if not pacote:
            return None
        dados += pacote
    return dados


def servidor():
    HOST = "0.0.0.0"
    PORT = 5001
    BASE_DIR = criar_diretorio_data()
    janela = PolaroidWindow()

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind((HOST, PORT))
    sock.listen(5)
    print(f"Servidor ouvindo em {HOST}:{PORT}")

    while True:
        print("Aguardando conexão...")
        conn, addr = sock.accept()
        print(f"Conexão estabelecida com {addr}")

        try:
            # Recebe 4 bytes com tamanho da imagem
            tamanho_bytes = receber_tudo(conn, 4)
            if not tamanho_bytes:
                print("Cliente desconectou antes do envio.")
                conn.close()
                continue

            tamanho = struct.unpack("!I", tamanho_bytes)[0]
            imagem_bytes = receber_tudo(conn, tamanho)
            if not imagem_bytes:
                print("Erro ao receber imagem.")
                conn.close()
                continue

            np_array = np.frombuffer(imagem_bytes, dtype=np.uint8)
            img = cv2.imdecode(np_array, cv2.IMREAD_COLOR)
            if img is None:
                print("Falha ao decodificar a imagem!")
                conn.close()
                continue

            caminho, nome_arquivo = caminho_imagem()
            cv2.imwrite(caminho, img)
            print(f"Imagem salva em: {caminho}")

            # Exibe imagem com legenda responsiva
            janela.mostrar(img, nome_arquivo)

        except Exception as e:
            print(f"Erro: {e}")

        finally:
            conn.close()
            print("Conexão encerrada.")

    janela.root.mainloop()  # mantém a janela aberta

if __name__ == "__main__":
    servidor()
