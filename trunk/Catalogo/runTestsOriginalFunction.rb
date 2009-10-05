#!/usr/bin/ruby
nrRepeticoes = 60
url='http://172.31.7.13:38080/Catalogo/HashTableServlet?acao=inserir_'

urlParte='/Catalogo/HashTableServlet?acao=inserir_'

#numClientes = [1,10,20,30,40,50,60,70,80,90,100]
numClientes = [50,60,70,80,90,100]
tam_mens = [10,100,1000,10000]

mode = 'original'


def executarTeste(mode, clusterSize, numClientes, tam_mens, nrRepeticoes, urlVetor)

	resultFolder="/home/objectweb/Desktop/testes/#{mode}/#{clusterSize}"

	numClientes.each{ |clienteAtual|
	  tam_mens.each{ |tam_atual|
	    nrRepeticoes.times{ |i|
	    
#	      puts "\n\n============================================================"
#	      puts "====   INICIANDO ITERAÇÃO #{i + 1} de #{nrRepeticoes}".ljust(61) + "="
#	      puts "====   NÚMERO DE CLIENTE: #{clienteAtual}".ljust(60) + "="
#	      puts "====   TAMANHO DA MENSAGEM: #{tam_atual}".ljust(60) + "="
#	      puts "====   ORIGINAL".ljust(59) + "="
#	      puts "============================================================\n\n"

		urlVetor.each{ |lUrl|
			system ("ab -d -n #{clienteAtual} -c #{clienteAtual} -g #{resultFolder}/results_#{clienteAtual}_#{tam_atual}_#{i+1}.txt #{lUrl}#{tam_atual}")
		}
	    }
	    puts "Terminando para #{clienteAtual} clientes e tamanho #{tam_atual}..."
	    `sleep 5`
	  }
	}   
end

executarTeste(mode, 2, numClientes, tam_mens, nrRepeticoes, ["http://172.31.7.13:38080#{urlParte}", "http://172.31.7.14:38080#{urlParte}"])

executarTeste(mode, 4, numClientes, tam_mens, nrRepeticoes, ["http://172.31.7.13:38080#{urlParte}", "http://172.31.7.14:38080#{urlParte}", "http://172.31.7.120:38080#{urlParte}", "http://172.31.7.56:38080#{urlParte}"])

executarTeste(mode, 6, numClientes, tam_mens, nrRepeticoes, ["http://172.31.7.13:38080#{urlParte}", "http://172.31.7.14:38080#{urlParte}", "http://172.31.7.120:38080#{urlParte}", "http://172.31.7.56:38080#{urlParte}", "http://172.31.7.113:38080#{urlParte}", "http://172.31.7.114:38080#{urlParte}"])




