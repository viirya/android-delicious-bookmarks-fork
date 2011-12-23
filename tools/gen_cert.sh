echo | openssl s_client -connect api.del.icio.us:443 2>&1 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > mycert.pem
