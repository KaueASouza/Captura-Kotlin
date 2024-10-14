package Captura

import com.github.britooo.looca.api.core.Looca
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate

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

    fun criarTabela(){
        jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS novaScan.Componentes(
            id INT AUTO_INCREMENT PRIMARY KEY,
            fkDispositivo INT,
            nome VARCHAR(45),
            fabricante VARCHAR(45),
            unidadeMedida VARCHAR(3),
            tempoAtv DATETIME,
            bytesEnviados BIGINT,
            bytesRecebidos BIGINT,
            FOREIGN KEY (fkDispositivo) REFERENCES dispositivo(id)
        );
        """.trimIndent())
    }

    fun inserirDados(capturarDados: DadosSistema): Boolean {
        val interfaceDeConexaoPrincipal = interfaces.filter { it.nomeExibicao.contains("211") }
        if (interfaceDeConexaoPrincipal.isNotEmpty()) {
            val bytesEnviados = interfaceDeConexaoPrincipal[0].bytesEnviados
            val bytesRecebidos = interfaceDeConexaoPrincipal[0].bytesRecebidos
            val bytesEnviadosConvertido = bytesEnviados / (1024 * 1024)
            val bytesRecebidosConvertido = bytesRecebidos / (1024 * 1024)

            val currentTime = java.time.LocalDateTime.now()
            val qtdLinhasAfetadas = jdbcTemplate.update(
                """ 
                INSERT INTO componentes(fkDispositivo,nome, fabricante,unidadeMedida, tempoAtv, bytesEnviados, bytesRecebidos) 
                VALUES (2,"Wi-fi 6E AX211","Intel","MB", ?,?, ?)
                """,
                currentTime,bytesEnviadosConvertido, bytesRecebidosConvertido
            )
            return qtdLinhasAfetadas > 0
        } else {
            println("Nenhuma interface de conexão principal encontrada.")
            return false
        }
    }

    fun listar(): List<Componentes> {
        return jdbcTemplate.query(
            "SELECT * FROM Repositorio.Componentes",
            BeanPropertyRowMapper(Componentes::class.java)
        )
    }

    fun atualizaPorId(id: Int, novaMedida: Componentes): Boolean {
        val qtdLinhasAfetadas = jdbcTemplate.update(
            """
                UPDATE Repositorio.Componentes SET
                fkDispositivo = ?,
                nome = ?,
                fabricante = ?,
                unidadeMedida = ?,
                tempoAtv = ?,
                bytesEnviados = ?,
                bytesRecebidos = ?
                WHERE id = ?
            """,
            novaMedida.fkDispositivo,
            novaMedida.nome,
            novaMedida.fabricante,
            novaMedida.unidadeMedida,
            novaMedida.tempoAtv,
            novaMedida.bytesEnviados,
            novaMedida.bytesRecebidos,
            id
        )
        return qtdLinhasAfetadas > 0
    }

    fun capturarDados(): DadosSistema {
        val interfaces = looca.rede.grupoDeInterfaces.interfaces
        val interfacePrincipal = interfaces.firstOrNull { it.nomeExibicao.contains("211") } ?: interfaces[0]
        val bytesEnviados = interfacePrincipal.bytesEnviados
        val bytesRecebidos = interfacePrincipal.bytesRecebidos

        return DadosSistema(
            nomeInterface = interfacePrincipal.nomeExibicao,
            bytesEnviados = bytesEnviados / (1024 * 1024), // Conversão para MB
            bytesRecebidos = bytesRecebidos / (1024 * 1024) // Conversão para MB
        )
    }

    data class DadosSistema(
        val nomeInterface: String,
        val bytesEnviados: Long,
        val bytesRecebidos: Long
    )

    data class Componentes(
        val fkDispositivo: Int,
        val nome: String,
        val fabricante: String,
        val unidadeMedida: String,
        val tempoAtv: String,
        val bytesEnviados: Long,
        val bytesRecebidos: Long
    )
}
