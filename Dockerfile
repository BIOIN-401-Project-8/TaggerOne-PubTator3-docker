FROM nvcr.io/nvidia/tensorflow:24.02-tf2-py3

RUN apt-get update && apt-get install -y default-jre && rm -rf /var/lib/apt/lists/*

COPY . /app
WORKDIR /app
RUN pip install --no-cache-dir -r requirements.txt