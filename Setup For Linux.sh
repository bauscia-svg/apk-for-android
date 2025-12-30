echo "\e[34mWelcome to the setup script!\e[0m"
echo "Updating and upgrading the system..."
sudo apt update && sudo apt upgrade
echo "System updated and upgraded."
echo "Installing essential software..."
sudo apt install snapd
sudo apt install build-essential
sudo apt install cmake
sudo apt install curl
sudo apt install wget
sudo apt install gparted
sudo snap install powershell
sudo snap install postman
echo "Essential software installed."
echo "Installing Python and setting up virtual environment..."
sudo apt install python3
echo "Python3 installed."
echo "Installing python virtual environment and pip3..."
sudo apt install python3-pip
pip3 install requests
echo "pip3 and requests library installed."
echo "Installing python3-venv..."
sudo apt install python3-venv
echo "Python, python3-venv, and pip3 set up and installed."
echo "Installing Flatpak and configuring repositories..."
sudo apt install flatpak
sudo flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo
sudo apt install gnome-software-plugin-flatpak
sudo flatpak update
echo "Flatpak installed and configured."
echo "Installing Visual Studio Code..."
sudo snap install code
echo "Installing Git and configuring user name..."
sudo apt install git
git config --global user.name "Alex"
echo "Git user name configured."
echo "Installing Node.js and npm..."
sudo apt install nodejs
sudo apt install npm
echo "Node.js and npm installed."
echo "Installing Docker..."
sudo apt install docker.io
echo "Docker installed."
echo "Installing additional software..."
sudo apt install qbittorrent
sudo snap install vlc
sudo snap install spotify
sudo snap install thunderbird
sudo snap install g4music
sudo snap install gemini-desktop
echo "Additional software installed."
echo "Performing final system update and cleanup..."
sudo apt autoremove
sudo apt clean
sudo updatedb
sudo apt update && sudo apt upgrade
echo "Final system update and cleanup completed."
echo "All tasks completed successfully."
echo "Script finished successfully."
echo "System is ready to use."
echo "Enjoy your development environment!"
echo "Goodbye!"


echo -e "\e[1mTesto in grassetto e più grande\e[0m"
echo -e "\e[34mTesto blu\e[0m" # Codice 34 per il blu
echo -e "\e[32mTesto verde\e[0m" # Codice 32 per il verde
echo -e "\e[31mTesto rosso\e[0m" # Codice 31 per il rosso
echo -e "\e[33mTesto giallo\e[0m" # Codice 33 per il giallo