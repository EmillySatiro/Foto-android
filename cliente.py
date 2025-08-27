import socket
import struct

# Caminho da imagem que você quer enviar
CAMINHO_IMAGEM = "/home/emilly/Trabalho 2 SD/Foto-android/servidor/__pycache__/Imagem do WhatsApp de 2025-08-21 à(s) 22.47.16_ff8556ce.jpg"  # substitua pelo arquivo que você quer enviar

# IP e porta do servidor
HOST = "127.0.0.1"  # se o servidor estiver na mesma máquina
PORT = 5001

def enviar_imagem():
    # Lê a imagem em bytes
    with open(CAMINHO_IMAGEM, "rb") as f:
        img_bytes = f.read()

    tamanho = len(img_bytes)
    print(f"Enviando {tamanho} bytes para o servidor...")

    # Conecta no servidor
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((HOST, PORT))

    try:
        # Primeiro envia o tamanho (4 bytes, big-endian)
        sock.sendall(struct.pack("!I", tamanho))
        # Depois envia os bytes da imagem
        sock.sendall(img_bytes)
        print("✅ Imagem enviada com sucesso!")
    except Exception as e:
        print(f"Erro ao enviar imagem: {e}")
    finally:
        sock.close()

if __name__ == "__main__":
    enviar_imagem()
