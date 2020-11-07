def pull(String key){
    def file = readJSON file: "resources/config.json"
    def config = file["$key"]
    return config
}