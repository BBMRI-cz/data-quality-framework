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
COPY requirements.txt requirements-imaging.txt /tmp/
RUN pip install --no-cache-dir --no-input \
        -r /tmp/requirements.txt \
        -r /tmp/requirements-imaging.txt \
    && rm /tmp/requirements.txt /tmp/requirements-imaging.txt
RUN groupadd --gid 9999 sandbox \
    && useradd --uid 9999 --gid sandbox --shell /usr/sbin/nologin --create-home sandbox
WORKDIR /sandbox
COPY omicsbox /sandbox/omicsbox
COPY main.py /sandbox/main.py
USER sandbox
# python:3.13-slim ships no curl/wget, so probe via urllib.
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=10s \
    CMD python -c "import sys,urllib.request; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/health').status==200 else 1)"
EXPOSE 8000
CMD ["python", "main.py"]
