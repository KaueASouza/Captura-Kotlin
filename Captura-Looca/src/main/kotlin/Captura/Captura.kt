package Captura

import com.github.britooo.looca.api.core.Looca
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject

class LoocaMonitor {
    lateinit var jdbcTemplate: JdbcTemplate

    val looca = Looca()
    val interfaces = looca.rede.grupoDeInterfaces.interfaces

    fun configurar() {
        try {
            val datasource = BasicDataSource()
            datasource.driverClassName = "com.mysql.cj.jdbc.Driver"
            datasource.url = "jdbc:mysql://localhost:3306/novaScan"
            datasource.username = "root"
            datasource.password = "CivicSi2007"
            jdbcTemplate = JdbcTemplate(datasource)
        } catch (e: Exception) {
            println("Erro ao configurar o datasource: ${e.message}")
            e.printStackTrace()
        }
    }

    // Função para validar o login
    private fun validarLogin(nome: String, senha: String): Boolean {
        val sql = "SELECT * FROM usuario WHERE nome = ? AND senha = ?"

        // Verifica se existe algum usuário com o nome e senha fornecidos
        val usuarios = jdbcTemplate.queryForList(sql, nome, senha)

        // Se a lista não estiver vazia, o login foi bem-sucedido
        return usuarios.isNotEmpty()
    }

    // Função para solicitar o login até ser bem-sucedido
    fun realizarLogin() {
        while (true) {
            print("Digite seu nome: ")
            val nome = readlnOrNull() ?: continue

            print("Digite sua senha: ")
            val senha = readlnOrNull() ?: continue

            if (validarLogin(nome, senha)) {
                println("Login bem-sucedido! Bem-vindo, $nome.")
                break // Login válido, prossegue com o código
            } else {
                println("Usuário ou senha inválidos. Tente novamente.")
            }
        }
    }
    fun UltimaMaquina(): Int {
        return jdbcTemplate.queryForObject(
            "SELECT last_insert_id(id) FROM dispositivo WHERE fkEmpresa = ?;",
            BeanPropertyRowMapper(Int::class.java)
        )
    }
    
    // Função para inserir dados de rede no banco
    fun inserirDados(capturarDados: DadosSistema): Boolean {
        val interfaceDeConexaoPrincipal = interfaces.filter { it.nomeExibicao.contains("211") }
        if (interfaceDeConexaoPrincipal.isNotEmpty()) {
            val bytesEnviados = interfaceDeConexaoPrincipal[0].bytesEnviados
            val bytesRecebidos = interfaceDeConexaoPrincipal[0].bytesRecebidos
            val bytesEnviadosConvertido = bytesEnviados / (1024 * 1024)
            val bytesRecebidosConvertido = bytesRecebidos / (1024 * 1024)

            val currentTime = java.time.LocalDateTime.now()
            val BytesRec = jdbcTemplate.update(
                """ 
                INSERT INTO log(valor, dataHora, descricao, fkComponente, fkDispositivo) 
                VALUES (?,?,"Bytes Recebidos",?,?)
                """,
                bytesRecebidosConvertido, currentTime, 1, 1
            )
            val BytesEnv = jdbcTemplate.update(
                """ 
                INSERT INTO log(valor, dataHora, descricao, fkComponente, fkDispositivo) 
                VALUES (?,?,"Bytes Enviados",?,?)
                """,
                bytesEnviadosConvertido, currentTime, 1, 1
            )
            return BytesRec > 0
        } else {
            println("Nenhuma interface de conexão principal encontrada.")
            return false
        }
    }


    // Função para capturar dados de rede
    fun capturarDados(): DadosSistema {
        val interfaces = looca.rede.grupoDeInterfaces.interfaces
        val interfacePrincipal = interfaces.firstOrNull { it.nomeExibicao.contains("211") } ?: interfaces[0]
        val bytesEnviados = interfacePrincipal.bytesEnviados
        val bytesRecebidos = interfacePrincipal.bytesRecebidos
        val valor: Double
        val fkComponente: Int
        val fkDispositivo: Int

        return DadosSistema(
            nomeInterface = interfacePrincipal.nomeExibicao,
            BytesEnviados = bytesEnviados / (1024 * 1024), // Conversão para MB
            BytesRecebidos = bytesRecebidos / (1024 * 1024) // Conversão para MB
        )
    }

    data class DadosSistema(
        val nomeInterface: String,
        val BytesEnviados: Long,
        val BytesRecebidos: Long
    )

    data class log(
        val valor: Int,
        val dataHora: String,
        val BytesRecebidos: Int,
        val BytesEnviados: Int,
        val fkComponente: Int,
        val fkDispositivo: Int,

    )
}

