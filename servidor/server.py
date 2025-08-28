import socket
import struct
import cv2
import numpy as np
import os
import datetime
import tkinter as tk
from PIL import Image, ImageTk
from threading import Thread

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

class JanelaPolaroid:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("BibSys")
        self.root.configure(bg="black")
        self.root.geometry("900x700")
        self.root.resizable(True, True)

        self.label = tk.Label(self.root, bg="black")
        self.label.pack(expand=True, fill="both")

        self.img_rgb = np.ones((400, 600, 3), dtype=np.uint8) * 255
        fonte = cv2.FONT_HERSHEY_SIMPLEX
        cv2.putText(self.img_rgb, "Aguardando foto...", (50, 200), fonte, 1.2, (0, 0, 0), 2, cv2.LINE_AA)
        self.atualizar_imagem(self.img_rgb, "Aguardando foto...")

        self.root.bind("<Configure>", lambda e: self.atualizar_imagem(self.img_rgb, "Aguardando foto..."))

    def atualizar_imagem(self, img_cv2, legenda="Imagem"):
        if not isinstance(img_cv2, np.ndarray):
            return  
        img_rgb = cv2.cvtColor(img_cv2, cv2.COLOR_BGR2RGB) if img_cv2.shape[2] == 3 else img_cv2

        largura_disp = self.label.winfo_width()
        altura_disp = self.label.winfo_height()
        if largura_disp < 10 or altura_disp < 10:
            return

        escala_w = largura_disp / img_rgb.shape[1]
        escala_h = altura_disp / img_rgb.shape[0]
        escala = min(escala_w, escala_h) * 0.85
        nova_largura = max(1, int(img_rgb.shape[1] * escala))
        nova_altura = max(1, int(img_rgb.shape[0] * escala))
        img_resized = cv2.resize(img_rgb, (nova_largura, nova_altura))

        borda_cima, borda_lados, borda_baixo = 40, 40, 70
        canvas = np.ones(
            (nova_altura + borda_cima + borda_baixo,
             nova_largura + 2 * borda_lados, 3), dtype=np.uint8
        ) * 255

        x_offset = borda_lados
        y_offset = borda_cima
        canvas[y_offset:y_offset+nova_altura, x_offset:x_offset+nova_largura] = img_resized

        fonte = cv2.FONT_HERSHEY_SIMPLEX
        escala_fonte = max(1, canvas.shape[1] // 600)
        espessura = max(1, escala_fonte)
        (text_largura, text_altura), _ = cv2.getTextSize(legenda, fonte, escala_fonte, espessura)
        x = (canvas.shape[1] - text_largura) // 2
        y = canvas.shape[0] - (borda_baixo // 3)
        cv2.putText(canvas, legenda, (x, y), fonte, escala_fonte, (0, 0, 0), espessura, cv2.LINE_AA)

        img_pil = Image.fromarray(canvas)
        img_tk = ImageTk.PhotoImage(img_pil)
        self.label.imgtk = img_tk
        self.label.config(image=img_tk)

def servidor():
    HOST = "0.0.0.0"
    PORT = 5001
    criar_diretorio_data()

    janela = JanelaPolaroid()

    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.bind((HOST, PORT))
    sock.listen(5)
    print(f"Servidor ouvindo em {HOST}:{PORT}")

    def aceitar_conexoes():
        while True:
            print("Aguardando conexão...")
            conn, addr = sock.accept()
            print(f"Conexão estabelecida com {addr}")
            try:
                tamanho_bytes = conn.recv(4)
                if not tamanho_bytes:
                    conn.close()
                    continue
                tamanho = struct.unpack("!I", tamanho_bytes)[0]
                imagem_bytes = b""
                while len(imagem_bytes) < tamanho:
                    pacote = conn.recv(tamanho - len(imagem_bytes))
                    if not pacote:
                        break
                    imagem_bytes += pacote
                if len(imagem_bytes) != tamanho:
                    conn.close()
                    continue
                np_array = np.frombuffer(imagem_bytes, dtype=np.uint8)
                img = cv2.imdecode(np_array, cv2.IMREAD_COLOR)
                if img is None:
                    conn.close()
                    continue
                caminho, nome_arquivo = caminho_imagem()
                cv2.imwrite(caminho, img)
                print(f"Imagem salva em: {caminho}")
                janela.root.after(0, lambda img=img, nome=nome_arquivo: janela.atualizar_imagem(img, nome))
            finally:
                conn.close()
                print("Conexão encerrada.")

    Thread(target=aceitar_conexoes, daemon=True).start()
    janela.root.mainloop()

if __name__ == "__main__":
    servidor()