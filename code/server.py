__author__ = 'Jebe'
# server.py
import math
import os
import socket
import time
import threading
import subprocess
import ipaddress
from bluetooth import *
import RPi.GPIO as GPIO
import datetime
import select
import subprocess

dn = os.path.dirname(os.path.realpath(__file__))

GPIO.setwarnings(False)

DATE_TIME_FORMAT = "%d/%m/%Y %H:%M:%S"
TIME_FORMAT = "%H:%M:%S"
# Duration of the Bluetooth scan
BT_SCAN_TIMEOUT = 60
BT_ANSWER_TIMEOUT = 30

# The uuid and address to identify the bluetooth service running on the mobile
UUID = "6725bf90-b304-4fb8-a413-ca4d4f162d69"
# Nexus 4
BT_SERVER_ADDR = "40:B0:FA:15:59:75"
# Tablet
#BT_SERVER_ADDR = "22:22:91:69:79:04"

#Communication messages
CONNECT_CMD = "connect"
CONNECT_ACK = "connectACK"
# REGISTER_OK = "registerOK"

BUFFER_SIZE = 1024
UDP_IP_DEST = "10.0.0.20"
UDP_IP_LOC = "10.0.0.1"
UDP_PORT = 8082

# Door lock
BOLT_PIN = 23
# Switch that checks if the door is closed
SWITCH_PIN = 24
# LED to indicate an closed door
RED_LED_PIN = 25
# LED to indicate an open door
GREEN_LED_PIN = 18
# Button to request the door to open
BUTTON_PIN = 27
# LED that indicates if hostapd and isc_dhcp services are active
SERVICES_OK_PIN = 22
# Passive infrared input
PIR_PIN = 14

devices_dictionary = []
devices_dictionary = dict(devices_dictionary)

client_addr = None
busy = False
bt_service = None
bt_busy = False

GPIO.setmode(GPIO.BCM)
GPIO.setup(BOLT_PIN, GPIO.OUT)
GPIO.setup(RED_LED_PIN, GPIO.OUT)
GPIO.setup(GREEN_LED_PIN, GPIO.OUT)
GPIO.setup(SERVICES_OK_PIN, GPIO.OUT)

GPIO.setup(BUTTON_PIN, GPIO.IN)
GPIO.setup(SWITCH_PIN, GPIO.IN)
GPIO.setup(PIR_PIN, GPIO.IN)

GPIO.output(BOLT_PIN, False)
GPIO.output(RED_LED_PIN, True)
GPIO.output(SERVICES_OK_PIN, False)


log_file_name = "./log/" + datetime.datetime.now().strftime("%Y-%m-%d_%H-%M-%S") + ".log"
open(log_file_name, 'w').close()


# ----------------------------- File Methods -----------------------------------


def log_to_file(log_data):
    log_time = datetime.datetime.now().strftime(TIME_FORMAT)
    log_file = open(log_file_name, 'a')
    log_file.write(log_time + " " + log_data + '\n')
    log_file.close()
    print log_data


def read_pass(device_id):
    with open('devices.txt', 'r') as f:
        for line in f:
            if device_id in line:
                device_pass = line.split(" ")[2]
                return device_pass


def make_dictionary():
    # Creates a dictionary where each entry is a correspondence BT address - Mac address
    global devices_dictionary
    log_to_file("Creating devices dictionary")
    with open('devices.txt', 'r') as f:
        for line in f:
            device_info = line.split(" ")
            devices_dictionary[device_info[0]] = device_info[1]
    print devices_dictionary


# ----------------------------- Bluetooth Methods ------------------------------


def find_bt_service(uuid, addr):
    global bt_service
    service_matches = find_service(uuid=uuid, address=addr)
    results = len(service_matches)
    if results > 0:
        bt_service = service_matches[0]
    return results


def connect_to_bt(host, port):
    global bt_busy
    bluetooth_socket = BluetoothSocket(RFCOMM)
    try:
        bluetooth_socket.connect((host, port))
    except:
        log_to_file("Error in bluetooth_socket.connect")
    log_to_file("Sending connection request to mobile device ")
    bluetooth_socket.send(CONNECT_CMD)
    bluetooth_socket.setblocking(0)
    ready = select.select([bluetooth_socket], [], [], BT_ANSWER_TIMEOUT)

    if ready[0]:
        try:
            answer = bluetooth_socket.recv(1024)
            if answer == CONNECT_ACK:
                log_to_file("Bluetooth answer from mobile device -> " + str(answer))
        except:
            log_to_file("Error in  bluetooth_socket.recv(1024)")
    else:
        log_to_file("Bluetooth timeout waiting for answer")
    bluetooth_socket.close()
    bt_busy = False

# ----------------------------- Bolt Methods ------------------------------


def close_door():
    GPIO.output(BOLT_PIN, False)
    report_state()
    listen_for_button()


def open_door(command_array):
    # "allow" requires at least 3 parameters: Command + Mac + BT_address + Duration and optionally pass
    array_len = len(command_array)
    pass_required = False
    pass_matches = True
    if array_len > 3:
        mac_address = command_array[1]
        duration = command_array[3]
        if array_len == 5:
            received_pass = command_array[4]
            pass_required = True

        if pass_required:
            log_to_file("Password is required to open, checking password")
            stored_pass = read_pass(mac_address).replace("\n", "")
            if stored_pass == received_pass:
                log_to_file("Password matches")
            else:
                pass_matches = False
                log_to_file("Wrong password")

        if pass_matches:
            log_to_file("Opening authorized by mobile device " + str(mac_address))
            GPIO.output(BOLT_PIN, True)
            report_state()
            time.sleep(float(duration))
    close_door()


def report_state():
    time.sleep(0.2)
    is_close = GPIO.input(SWITCH_PIN) == GPIO.HIGH
    if is_close:
        switch_leds(True)
        bolt_state = "close"
        log_to_file("-----------------CLOSE----------------------")
    else:
        switch_leds(False)
        bolt_state = "open"
        log_to_file("------------------OPEN----------------------")

    try:
        wifi_socket_client.sendto(bolt_state, (UDP_IP_DEST, UDP_PORT))
    except:
        print "Unexpected error:", sys.exc_info()


def switch_leds(is_red):
    GPIO.output(RED_LED_PIN, is_red)
    GPIO.output(GREEN_LED_PIN, not is_red)

# ----------------------------- Other Methods -----------------------------


def is_associated(mac):
    p = subprocess.Popen("sudo hostapd_cli sta " + mac + " | grep ^" + mac, stdout=subprocess.PIPE, shell=True)
    (output, err) = p.communicate()
    output = output.rstrip()
    return err is None and output.rstrip() == mac


def request_auth():
    log_to_file("Sending auth request")
    # TODO check to who must be sent
    # Check who is in the nearby and is authorized to open and send him the request
    wifi_socket_client.sendto("authRequest", (UDP_IP_DEST, UDP_PORT))
    listen_on_wifi()

# ----------------------------- 4 Main Methods ------------------------------


def check_services():
    log_to_file("Checking services")
    services_output = subprocess.check_output(['ps', '-A'])
    hcitool_result = subprocess.Popen(["hcitool", "dev"], stdout=subprocess.PIPE)
    hci_devices, err = hcitool_result.communicate()
    GPIO.output(SERVICES_OK_PIN, False)

    if len(hci_devices) > 10:
        log_to_file("Bluetooth " + hci_devices)
    else:
        log_to_file("ERROR: No bluetooth device found!")
        return False

    if 'dhcp' in services_output:
        log_to_file("Isc-dhcp-server is running")
    else:
        log_to_file("ERROR: Problem starting DHCP Server!")
        return False

    if 'hostapd' in services_output:
        log_to_file("Hostapd is running")
    else:
        log_to_file("ERROR: Problem starting Hostapd!")
        return False

    GPIO.output(SERVICES_OK_PIN, True)
    return True


def listen_on_wifi():
    # TODO set a timer here
    log_to_file("Starting listener on WIFI port {}".format(UDP_PORT))
    while 1:
        try:
            data, addr = wifi_socket_server.recvfrom(BUFFER_SIZE)
        except:
            log_to_file("Error on wifi_socket_server")
        if data:
            log_to_file("Received: " + data)
            content = data.split('>>')
            log_to_file("Answer from mobile device ->" + content[0])
            if content[0] == "allow" or content[0] == "deny" or content[0] == "register":
                try:
                    actions[content[0]](content)
                except:
                    print "\nError"


def start_bt_scan(timeout):
    global bt_busy
    global bt_service
    bt_busy = True
    log_to_file("Starting Bluetooth scan")
    timeout += time.time()
    while time.time() < timeout:
        # TODO make a loop to check all the devices in devices.txt instead of just BT_SERVER_ADDR
        # If the device is associated or no one is in the Bluetooth range pass till timeout the scan
        if find_bt_service(UUID, BT_SERVER_ADDR) < 1 or is_associated(devices_dictionary[BT_SERVER_ADDR]):
            pass
        else:
            if bt_service is not None:
                log_data = "\nBluetooth service found: {0} on {1} port: {2}".format(str(bt_service["name"]),
                                                                                str(bt_service["host"]),
                                                                                str(bt_service["port"]))
            log_to_file(log_data)
            connect_to_bt(bt_service["host"], bt_service["port"])
            break
    else:
        log_to_file("Bluetooth scan timeout")
        bt_busy = False


def start_pir_scan():
    global bt_busy
    log_to_file("\nStarting passive infrared sensor")
    while True:
        if GPIO.input(PIR_PIN) and not bt_busy:
            log_to_file("Passive infrared detected movement")
            start_bt_scan(BT_SCAN_TIMEOUT)


def listen_for_button():
    log_to_file("\nStarting button listener")
    global busy
    while not busy:
        if GPIO.input(BUTTON_PIN):
            if GPIO.HIGH:
                request_auth()
                busy = True
            time.sleep(0.3)

# --------------------------Main Thread---------------------------------------
log_to_file("Initializing BoltControl")

actions = {"allow": open_door, "deny": open_door}

wifi_socket_server = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
wifi_socket_client = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
wifi_socket_server.bind(("", UDP_PORT))

# If the hostapd and dhcp services are running start the bluetooth and wifi threads
if check_services():
    make_dictionary()
    try:
        pir_thread = threading.Thread(target=start_pir_scan)
        pir_thread.daemon = True
        pir_thread.start()

        wifi_thread = threading.Thread(target=listen_on_wifi)
        wifi_thread.daemon = True
        wifi_thread.start()

    except:
        print "\nThread error"

    listen_for_button()
else:
    exit()
