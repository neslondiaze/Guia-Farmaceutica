#!/usr/bin/env python3
"""
Script de Administración - Generador de Licencias Offline para Guía Farmacéutica Venezuela.
Lee automáticamente la Llave Pública desde SecurityManager.java y valida el par criptográfico.
"""

import base64
import re
import os
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives import serialization, hashes

# Llave Privada RSA (PKCS#8 PEM) correspondiente a la Llave Pública en SecurityManager.java
PRIVATE_KEY_PEM = """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDLlPdh/iSjvhMV
+i8UYbsf2X+MwYLh2x039POnofj7i1XwOtACh/aho0qwVA4DDMUxH07cYCwGVS1N
DLZeiHx/9dwgXOzvI2ZyAqFnO2GWls/VTo8NzCowBOO3Y/3SWGRmGS6FCgcriHMl
nnth8cMau9K5P+3lv/q/LUwMiF3epmk+rko6Qph8oK3KcM02u21e4WJmYtcqC+IZC
6O3rczwnhizpVeP8lbFkMkrMGeM4em9t0qXox30xBXS03bYUsVOuMwPqyZbsWJ+5
S3jCug2lyMlNBZuqkexY2XqOsUp7ffOlo9d44XqQyLwHAl7O0RYRYE2470i6bXT/
/dm4puIPAgMBAAECggEAAOE1g6h9fNenBw+vTf8+HinqcZYRSR73rI46bB1r3gQA
7Kpt/uGaum57z1dA2D9UR51DBS2SAyHLGFIkJGeilUr5FvEcqDJKKj2PEcshfznk
aNLC4eRhkLBbHsFbD2878wgybN2MoMhUkKanEm9j7Gc4rM/OLojpnMKD31nXaO9n
yjlj+4qlylhv0U9o0/eN2TtXl1XRqurrMHO46v4OSexc+BCL1kfnqZBMo7XZgVcf
O3bPe/5FJfM7j1nyZXeIiz1VFoX/8wK51ij5pSlhzVrSS9zeQvJ9F74OeEoSPHh3
Bf6KoS/sxKmmrwW63eXz8/BvmfUyTghtjhRTXwrYQQKBgQD6rHWjjdJ/m53NBedm
nj+A+sz4sk17yjLUH5VkR7a7gx/IFz2Dbhi2HZshxmXjm/0Fd3S/ZrNDPXba3/tH
YOQt3UM2tLcGSlJ+/uCC412IuUUF3B4ZT1whUWEeJjIlm/hLRvEWGtXy1a1hC3XS
njFoqL4u/UqLw7bmBru2Hfqsm8wKBgQDP6FnWpJt1blYtaLZxwLcfyu6uY6+3o0U8
1MMc4WjnWa+2Jsgx41TlsxmB9m+lOZKXWnyALVRHfg9nxFYBT7TpdNhdQExaNJPT
RZ3CCuhNqLJcfq0uotkxJeOPFB7ZiTGBQ/kPttrBxqj2k8M/N5loAJMp4q8t1Ftf
sbbIrGjXdQKBgAlykoR1hed1T/84J76AXFhaG3uMDMuLlVrVTeYXpdVvXbVG2vSn
I5tJhl1BgvmPGXwpQmcsUblJCb+6DVhlWHQ6EJxxNyyvhGhw5rlIJHBQuz87So8d
npJVE4eom+mDcfgEoTVHN9R5P21b6/ZfP3l4UqKmvTaDS9NjkZONkbMLtAoGALOoX
LmWVKANUHq82atClPvsKISZr+ZIODRwxonWXwhMAAKvUJ+LinlTYt9jTObB8WLI1
n0Mrp9412cbyhYoAnUaez1ZqqDofjU7Gy/qrQMum7JB5PU/098clp+1C1N/sj+5t
SdXI4H/YVJyLW4bZXzxwjmXoTlBIBR5RTHzikVkCgYEAiFAA684cYvInCIWsXj7E
+1xPo1a+2XXoSZtfVfyXROHZMwUNjtElccNkiqUMfL3vIuCVfBo5hhuG0dS8emCM
ZEwul6RdAUACsCYyTw2oRZ+0XUo7JovanS/dVr7janSUmvTV8RrLvwectLR16Ew4
Pap09acap9nqnC/yR9nVHEA=
-----END PRIVATE KEY-----"""

def validar_con_security_manager():
    sec_path = os.path.join("android", "app", "src", "main", "java", "ve", "guiafarmaceutica", "app", "util", "SecurityManager.java")
    if not os.path.exists(sec_path):
        print(f"[!] Advertencia: No se encontró {sec_path}")
        return

    with open(sec_path, "r", encoding="utf-8") as f:
        content = f.read()

    match = re.search(r'PUBLIC_KEY_BASE64\s*=\s*"([^"]+)"', content)
    if not match:
        print("[!] Advertencia: No se pudo extraer PUBLIC_KEY_BASE64 de SecurityManager.java")
        return

    app_pub_b64 = match.group(1)

    private_key = serialization.load_pem_private_key(PRIVATE_KEY_PEM.encode('utf-8'), password=None)
    pub_der = private_key.public_key().public_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    script_pub_b64 = base64.b64encode(pub_der).decode('utf-8')

    if app_pub_b64 == script_pub_b64:
        print("[✔] Validación Criptográfica Exitosa: La Llave Pública en SecurityManager.java coincide 100% con la Llave Privada del script.")
    else:
        print("[❌] Error de Coincidencia: La Llave Pública en SecurityManager.java DIFERE de la Llave Privada de este script.")

def main():
    print("==================================================")
    print("  GUÍA FARMACÉUTICA VENEZUELA - ADMIN KEYGEN     ")
    print("==================================================")

    validar_con_security_manager()

    private_key = serialization.load_pem_private_key(PRIVATE_KEY_PEM.encode('utf-8'), password=None)

    while True:
        android_id = input("\nIntroduce el ANDROID_ID del cliente (ej. 068d30d287e15460) o 'salir': ").strip()
        if not android_id or android_id.lower() == 'salir':
            print("Saliendo del generador. ¡Hasta luego!")
            break

        signature = private_key.sign(
            android_id.encode('utf-8'),
            padding.PKCS1v15(),
            hashes.SHA256()
        )
        licencia_b64 = base64.b64encode(signature).decode('utf-8')

        print("\n==================================================")
        print(f" LÍCENCIA OFICIAL PARA EL ID: {android_id}")
        print("==================================================")
        print(licencia_b64)
        print("==================================================\n")

if __name__ == '__main__':
    main()
