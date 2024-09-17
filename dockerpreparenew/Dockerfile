FROM maximoplus/maximoplus-server
WORKDIR /mp
COPY maximo.ear .
RUN /mp/prepare.sh inst maximo.ear
RUN rm /mp/maximo.ear
RUN cp startdocker.sh deployment/inst/
COPY . deployment/inst/
WORKDIR /mp/deployment/inst
CMD ./startdocker.sh
EXPOSE 8080

