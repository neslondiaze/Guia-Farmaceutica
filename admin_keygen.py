#!/usr/bin/env python3
"""
Script de Administración - Generador de Licencias Offline para Guía Farmacéutica Venezuela.
Genera un par de llaves RSA (2048 bits), muestra la Llave Pública para pegar en SecurityManager.java,
y permite firmar digitalmente cualquier ANDROID_ID de cliente.
"""

import base64
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import serialization, hashes

def main():
    print("==================================================")
    print("  GUÍA FARMACÉUTICA VENEZUELA - ADMIN KEYGEN     ")
    print("==================================================")

    # 1. Generar par de llaves RSA de 2048 bits
    print("\n[+] Generando par de llaves criptográficas RSA (2048 bits)...")
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048
    )
    public_key = private_key.public_key()

    # Exportar llave pública en formato DER X.509 codificada en Base64
    pub_der = public_key.public_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    pub_b64 = base64.b64encode(pub_der).decode('utf-8')

    print("\n--------------------------------------------------")
    print("👉 COPIA ESTA LLAVE PÚBLICA EN SecurityManager.java:")
    print("--------------------------------------------------")
    print(pub_b64)
    print("--------------------------------------------------\n")

    while True:
        android_id = input("Introduce el ANDROID_ID del cliente (o presiona Enter para salir): ").strip()
        if not android_id:
            print("Saliendo del generador. ¡Hasta luego!")
            break

        # Firmar digitalmente el ANDROID_ID con la Llave Privada RSA generada
        signature = private_key.sign(
            android_id.encode('utf-8'),
            padding.PKCS1v15(),
            hashes.SHA256()
        )
        licencia_b64 = base64.b64encode(signature).decode('utf-8')

        print("\n==================================================")
        print(f" LICENCIA OFICIAL PARA EL ID: {android_id}")
        print("==================================================")
        print(licencia_b64)
        print("==================================================\n")

if __name__ == '__main__':
    main()
