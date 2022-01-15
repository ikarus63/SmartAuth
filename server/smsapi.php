<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

include "sms_gateway.php";

if(!isset($_GET["auth"])){
  die("You are not allowed to be here!");
}

$sender = new SMSGateway();
$response = $sender->send($_GET["phone"], "Váš ověřovací kód je " . $_GET["code"] . ". #SmartAuthapp");

print_r($response);

 ?>
