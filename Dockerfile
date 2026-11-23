# BarberAgenda - imagem para rodar na nuvem (Tomcat 10.1 + Java 21)
#
# Etapa 1: compila as classes Java usando as bibliotecas do proprio Tomcat.
FROM tomcat:10.1-jdk21-temurin AS build
WORKDIR /app
COPY src ./src
COPY web ./web
RUN mkdir -p web/WEB-INF/classes \
 && javac --release 21 -encoding UTF-8 \
      -cp "$CATALINA_HOME/lib/*:web/WEB-INF/lib/*" \
      -d web/WEB-INF/classes $(find src/java -name '*.java') \
 && rm -f web/WEB-INF/db.properties web/WEB-INF/ia.properties

# Etapa 2: imagem final, so com o Tomcat e o site ja compilado.
FROM tomcat:10.1-jre21-temurin
ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-Xms128m -Xmx384m -Duser.timezone=America/Sao_Paulo -Dfile.encoding=UTF-8"
RUN rm -rf $CATALINA_HOME/webapps/* \
 # Atras do Caddy: o Tomcat passa a enxergar https e o IP real do visitante
 && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto" />\n      </Host>#' $CATALINA_HOME/conf/server.xml
# Publicado como ROOT: o site abre direto em http://seu-endereco/
COPY --from=build /app/web $CATALINA_HOME/webapps/ROOT
EXPOSE 8080
CMD ["catalina.sh", "run"]
