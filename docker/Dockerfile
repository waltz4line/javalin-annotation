# Anaconda 기반 이미지 선택
FROM continuumio/anaconda3

# 필요한 패키지 업데이트 및 설치
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Airflow 설치
RUN pip install apache-airflow

# 추가 패키지 설치 (dependencies.req)
COPY dependencies.req /app/dependencies.req
RUN pip install -r /app/dependencies.req

# 커스텀 DAG와 플러그인 폴더 복사
COPY dags/ /usr/local/airflow/dags/
COPY plugins/ /usr/local/airflow/plugins/

# 작업 디렉토리 설정
WORKDIR /usr/local/airflow

# 포트 설정 (Airflow 기본 포트 8080)
EXPOSE 8080

# Airflow 초기화 및 웹 서버 실행
CMD ["bash", "-c", "airflow db init && airflow users create --username admin --password admin --firstname Admin --lastname User --role Admin --email admin@example.com && airflow webserver -p 8080"]

# CMD ["bash", "-c", "airflow db init && airflow users create --username admin --password admin --firstname Admin --lastname User --role Admin --email admin@example.com && airflow webserver -p 8080"]
# docker build -t airflow-anaconda .
# docker run -p 8080:8080 airflow-anaconda

# 실행시 볼륨 마운트
#docker run -p 8080:8080 \
#  -v $(pwd)/dags:/usr/local/airflow/dags \
#  -v $(pwd)/plugins:/usr/local/airflow/plugins \
# airflow-anaconda


# 모델 설치
#my_model/
#├── my_model/
#│   ├── __init__.py
#│   └── model.py  # 모델의 주요 코드가 여기에 포함됨
#└── setup.py

#from setuptools import setup, find_packages
#
#setup(
#    name='my_model',
#    version='0.1',
#    packages=find_packages(),
#    install_requires=[
#        # 여기에 모델에 필요한 추가 패키지(예: numpy, pandas 등)를 나열
#        'numpy',
#        'pandas',
#    ],
#)

#pip install .

# # Dockerfile에 패키지 복사 및 설치 명령 추가
  #COPY my_model/ /app/my_model/
  #RUN pip install /app/my_model/