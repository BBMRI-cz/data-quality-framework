FROM python:3.13-slim@sha256:c33f0bc4364a6881bed1ec0cc2665e6c53c87a43e774aaeab88e6f17af105e4f
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libopenslide0 \
        libgl1 \
        libglib2.0-0 \
        libsm6 \
        libxext6 \
        libxrender1 \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir --no-input -r /tmp/requirements.txt \
    && rm /tmp/requirements.txt
RUN groupadd --gid 9999 sandbox \
    && useradd --uid 9999 --gid sandbox --shell /usr/sbin/nologin --create-home sandbox
WORKDIR /sandbox
USER sandbox
COPY --chown=sandbox:sandbox main.py /sandbox/main.py
EXPOSE 8000
CMD ["python", "main.py"]
