gunicorn -w 1 --max-requests 1 -t 3600 -b 0.0.0.0:5000 wasdiProcessorServer:app
