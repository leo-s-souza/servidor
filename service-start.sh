#!/bin/bash
# My first script

sudo ./stop.sh
echo "CAPP fechado"
sudo systemctl restart capp.service
echo "Serviço capp reiniciado"
sudo systemctl enable capp.service
echo "Serviço capp liberado"
sudo systemctl daemon-reload
echo "Recarrecado daemon"
