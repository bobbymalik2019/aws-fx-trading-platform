chmod +x setup-linux.sh
./setup-linux.sh

git clone https://github.com/bobbymalik2019/aws-fx-trading-platform.git
cd aws-fx-trading-platform/fix-accepter-server

docker build -t fx-lp-simulator:1.0 .