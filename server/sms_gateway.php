<?php

class SMSGateway
{
    const AUTH_URL = "https://app.gosms.cz/oauth/v2/token";       // URL pro získání Access Tokenu
    const MESSAGE_URL = "https://app.gosms.cz/api/v1/messages/";  // URL pro odeslání zprávy
    const CLIENT_ID = "12987_1vmh5vgxm3nokws48w0scossw0sswwokog8cowg0g488ocggws";                                   // client id - Najdete v samoobsluze GoSMS - API https://app.gosms.cz/selfservice/channel/api
    const CLIENT_SECRET = "1cyalv8k9qjo8sgw0sscs4004woo00ws00kgk8gc4ks88ggw0s";                               // heslo - Najdete v samoobsluze GoSMS - API
    const DEFAULT_CHANNEL = 299862;                               // ID komunikačního kanálu - Dostupné kanály najdete v samoobsluze GoSMS - API

    private $accessToken;
    private $accessTokenExpiration = 0;

    /**
     * Odešle SMS
     * @param array $phoneNumbers
     * @param string $message
     * @param null|bool|int $channel Null pro výchozí kanál, false pro bez kanálu, číslo pro ruční volbu kanálu
     * @return bool Uspěšně odesláno
     * @throws \Exception
     */
    public function send($phoneNumbers, $message, $test = false, $channel = null, &$request = null, &$response = null)
    {

      if (!is_array($phoneNumbers)) {
          $phoneNumbers = array($phoneNumbers);
      }

        $stop = false;

        foreach ($phoneNumbers as $phoneNumber) {
            if (trim($phoneNumber) == '123456789'){
                $stop = true;
                break;
            }
        }

        if ($stop){
            return  true;
        }

        $unwanted_array = array('Š' => 'S', 'š' => 's', 'Ž' => 'Z', 'ž' => 'z', 'Č' => 'C', 'č' => 'c', 'À' => 'A', 'Á' => 'A', 'Â' => 'A', 'Ã' => 'A', 'Ä' => 'A', 'Å' => 'A', 'Æ' => 'A', 'Ç' => 'C', 'È' => 'E', 'É' => 'E',
            'Ê' => 'E', 'Ë' => 'E', 'Ě' => 'E', 'ě' => 'e', 'Ř' => 'R', 'ř' => 'r', 'Ì' => 'I', 'Í' => 'I', 'Î' => 'I', 'Ï' => 'I', 'Ñ' => 'N', 'Ò' => 'O', 'Ó' => 'O', 'Ô' => 'O', 'Õ' => 'O', 'Ö' => 'O', 'Ø' => 'O', 'Ù' => 'U',
            'Ú' => 'U', 'Û' => 'U', 'Ü' => 'U', 'Ý' => 'Y', 'Þ' => 'B', 'ß' => 'Ss', 'à' => 'a', 'á' => 'a', 'â' => 'a', 'ã' => 'a', 'ä' => 'a', 'å' => 'a', 'æ' => 'a', 'ç' => 'c',
            'è' => 'e', 'é' => 'e', 'ê' => 'e', 'ë' => 'e', 'ì' => 'i', 'í' => 'i', 'î' => 'i', 'ï' => 'i', 'ð' => 'o', 'ñ' => 'n', 'ò' => 'o', 'ó' => 'o', 'ô' => 'o', 'õ' => 'o',
            'ö' => 'o', 'ø' => 'o', 'ù' => 'u', 'ú' => 'u', 'û' => 'u', 'ý' => 'y', 'þ' => 'b', 'ÿ' => 'y');

        $request = array(
            "recipients" => $phoneNumbers,
            "message" => strtr($message, $unwanted_array),
            "expectedSendStart" => "now"
        );


        if (is_null($channel)) {
            $channel = self::DEFAULT_CHANNEL;
        }

        if ($channel) {
            $request["channel"] = $channel;
        }

        $accessToken = $this->getAccessToken();
        $url = self::MESSAGE_URL;
        if ($test == true) {
            $url .= "test";
        }
        $url .= "?access_token=" . $accessToken;

        $response = $this->post($url, $request);
        return $response;
    }


    /**
     * Získá access token
     * @return string
     * @throws Exception
     */
    private function getAccessToken()
    {
        if ($this->accessTokenExpiration <= time()) {
            $this->accessToken = null;
        }
        if (empty($this->accessToken)) {
            $response = $this->getLogin();
            if (isset($response->access_token)) {
                $this->accessToken = $response->access_token;
            }
            $this->accessTokenExpiration = time() - 1;
            if (isset($response->expires_in)) {
                $this->accessTokenExpiration += $response->expires_in;
            }
        }
        if (empty($this->accessToken)) {
            throw new \Exception("Nepodařilo se získat access token pro spojení s GoSMS.");
        }
        return $this->accessToken;
    }

    private function getLogin(&$url = null)
    {
        $queryParams = array(
            "client_id" => self::CLIENT_ID,
            "client_secret" => self::CLIENT_SECRET,
            "grant_type" => "client_credentials"
        );
        $url = self::AUTH_URL . "?" . http_build_query($queryParams);
        return $this->get($url);
    }

    private function get($url)
    {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_HEADER, false);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('content-type: application/json'));
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_URL, $url);
        $data = json_decode(curl_exec($ch));
        curl_close($ch);
        unset($ch);
        return $data;
    }

    private function post($url, $data)
    {
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_HEADER, false);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, array('content-type: application/json'));
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        $data = json_decode(curl_exec($ch));
        curl_close($ch);
        unset($ch);
        return $data;
    }
}
