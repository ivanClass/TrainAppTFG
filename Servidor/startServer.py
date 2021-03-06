import os
import shutil
import datetime
from bottle import post,route, request, static_file, run, template

from bottle import route, run, template
import os, os.path
from os import walk
import ProcesarDatos

import numpy as np
import pandas as pd
from numpy import genfromtxt
from sklearn.tree import DecisionTreeClassifier
from sklearn.tree import _tree

CONST_DIR_TMP = "./tmp/"

@route('/')
def home():
	procesaDatos()
	##return template('formUploadFile')

#PARA PRUEBAS DESDE LOCAL
def procesaDatos():
	ruta_timestamp = CONST_DIR_TMP + "2017_06_15(___)00_43_32_631000" + "/"
	ruta_timestamp_tmp = ruta_timestamp + "tmp/"
	#llamadaAprocesar(ruta_timestamp, ruta_timestamp_tmp)
	#juntar(ruta_timestamp_tmp)
	#concatenar(ruta_timestamp_tmp)

	feature_names=[ 'gyro_alpha_avg','gyro_beta_avg','gyro_gamma_avg','accel_x_avg','accel_y_avg','accel_z_avg','gyro_alpha_min','gyro_beta_min','gyro_gamma_min','accel_x_min','accel_y_min','accel_z_min','gyro_alpha_max','gyro_beta_max','gyro_gamma_max','accel_x_max','accel_y_max','accel_z_max','gyro_alpha_std','gyro_beta_std','gyro_gamma_std','accel_x_std','accel_y_std','accel_z_std','xy_cor','xz_cor','yz_cor','x_fft','y_fft','z_fft','gyro_alpha_med','gyro_beta_med','gyro_gamma_med','accel_x_med','accel_y_med','accel_z_med']

	directorioActual = os.getcwd()

	#os.chdir(ruta_timestamp_tmp)
	X = np.loadtxt("X_train_movil_aplaudirfinal.csv", delimiter=",")
	y = np.loadtxt("y_train_movil_aplaudirfinal.csv", delimiter="\n")
	#os.chdir(directorioActual)

	clf = DecisionTreeClassifier(criterion='entropy', max_depth=6, random_state=0, min_samples_split=2, min_samples_leaf=2)
	clf = clf.fit(X, y)

	codigo = "public int miMetodo(HashMap hashMap){\n"
	codigo = tree_to_code(clf, feature_names, codigo)
	codigo = codigo + "\n}"

	
	print codigo

#PARA ANDROID
@post('/subeDatos')
def subeDatos():
	peticion_time = ('{:%Y_%m_%d(___)%H_%M_%S_%f}'.format(datetime.datetime.now()))
	
	directorio_de_trabajo = CONST_DIR_TMP + peticion_time + "/"
	directorio_datos_procesados = directorio_de_trabajo + "tmp/"



	upload = request.files.getall('upload')

	
	save_path = CONST_DIR_TMP
	
	if not os.path.exists(save_path):
		os.makedirs(save_path)

	os.makedirs(directorio_de_trabajo)
	os.makedirs(directorio_datos_procesados)

	for file in upload:
		name, ext = os.path.splitext(file.filename)

		if ext not in ('.csv'):
			return 'File extension not allowed.'

		file_path = "{path}/{file}".format(path=directorio_de_trabajo, file=file.filename)
		file.save(file_path)

	arbol_cliente = creameClaseArbolString(directorio_de_trabajo, directorio_datos_procesados)

	#Tras terminar, eliminamos los ficheros copiados en tmp
	shutil.rmtree(directorio_de_trabajo, ignore_errors=True)

	return arbol_cliente



def creameClaseArbolString(ruta_timestamp, ruta_timestamp_tmp):
	##Procesamos los datos con las metricas
	llamadaAprocesar(ruta_timestamp, ruta_timestamp_tmp)
	juntar(ruta_timestamp_tmp)
	concatenar(ruta_timestamp_tmp)

	directorioActual = os.getcwd()
	os.chdir(ruta_timestamp_tmp)

	##creamos el arbol
	X = np.loadtxt("X_train_movil.csv", delimiter=",")
	y = np.loadtxt("y_train_movil.csv", delimiter="\n")

	os.chdir(directorioActual)
	
	clf = DecisionTreeClassifier(criterion='entropy', max_depth=6, random_state=0, min_samples_split=2, min_samples_leaf=2)
	clf = clf.fit(X, y)	
	
	feature_names=[ 'gyro_alpha_avg','gyro_beta_avg','gyro_gamma_avg','accel_x_avg','accel_y_avg','accel_z_avg','gyro_alpha_min','gyro_beta_min','gyro_gamma_min','accel_x_min','accel_y_min','accel_z_min','gyro_alpha_max','gyro_beta_max','gyro_gamma_max','accel_x_max','accel_y_max','accel_z_max','gyro_alpha_std','gyro_beta_std','gyro_gamma_std','accel_x_std','accel_y_std','accel_z_std','xy_cor','xz_cor','yz_cor','x_fft','y_fft','z_fft','gyro_alpha_med','gyro_beta_med','gyro_gamma_med','accel_x_med','accel_y_med','accel_z_med']
	
	codigo = "public int miMetodo(HashMap hashMap){\n"
	codigo = tree_to_code(clf,feature_names, codigo)
	codigo = codigo + "\n}"

	return codigo

def llamadaAprocesar(ruta_timestamp, ruta_timestamp_tmp):
	j = 0;

	for item in os.listdir(ruta_timestamp):
	    if os.path.isfile(os.path.join(ruta_timestamp, item)):
			if (j % 3 == 0):
				print(item)
				nombre = item.split('_')
				persona = nombre[0]
				actividad = nombre[1]
				ProcesarDatos.getStatisticsValues(('%s_%s' %(persona, actividad)), 3, ruta_timestamp, ruta_timestamp_tmp)

			j = j + 1;

def juntar(ruta_timestamp_tmp):
	dfOutX = pd.DataFrame()
	dfOutY = pd.DataFrame()

	directorioActual=os.getcwd()

	for (path, ficheros, archivos) in walk(ruta_timestamp_tmp):
		for i in  archivos:
			print(i)
			actividada=i.split('_')
			actividad=actividada[1]
		
			os.chdir(ruta_timestamp_tmp)
			dfX=pd.DataFrame()
			dfX = pd.read_csv(i, sep=';', index_col=0)

			os.chdir(directorioActual)
			dfY=pd.DataFrame()

			lista=list()

			d=0
			for d in range(len(dfX)):
				lista.append(actividad)
						
			dfY=pd.DataFrame(lista)
	  
			dfOutX = dfOutX.append(dfX)
			dfOutY = dfOutY.append(dfY)

	#os.chdir(directorioActual)
	os.chdir(ruta_timestamp_tmp)
	dfOutX.to_csv("X_train_movil.csv", index=False, header=False)
	dfOutY.to_csv("y_train_movil.csv", index=False, header=False)
	os.chdir(directorioActual)

def maximovalor(arr):
    maximo = 0
    for i in range(len(arr)):
        if(arr[i] > arr[maximo]):
            maximo = i
    return maximo

def tree_to_code(tree, feature_names, codigo):
    tree_ = tree.tree_
    feature_name = [
        feature_names[i] if i != _tree.TREE_UNDEFINED else "undefined!"
        for i in tree_.feature
    ]

    def recurse(node, depth, codigo):
        
        indent = "  " * depth
        if tree_.feature[node] != _tree.TREE_UNDEFINED:
            name = feature_name[node]
            threshold = tree_.threshold[node]
            codigo = codigo + "if(hashMap.get(\"{}\") <= {})\n".format(name, threshold)
            codigo = recurse(tree_.children_left[node], depth + 1, codigo)
            codigo = codigo + "else //if({} > {})\n".format(name, threshold)
            codigo = recurse(tree_.children_right[node], depth + 1, codigo)
        else:
            a = maximovalor(tree_.value[node][0])
            codigo = codigo + "return {};\n".format(int(a + 1))
        return codigo
        
    codigo = recurse(0, 1, codigo)
    return codigo

def concatenar(ruta_timestamp_tmp):
	infile = open('X_train_movil_aplaudirfinal.csv', 'r')
	datosAnteriores=infile.read()

	directorioActual=os.getcwd()
	os.chdir(ruta_timestamp_tmp)

	outfileNuevo = open('X_train_movil.csv', 'a')
	outfileNuevo.write(str(datosAnteriores))
	outfileNuevo.close()
	infile.close()

	os.chdir(directorioActual)
	
	#concatenar y
	
	infile = open('y_train_movil_aplaudirfinal.csv', 'r')
	datosAnteriores=infile.read()
	infile.close()

	os.chdir(ruta_timestamp_tmp)

	outfileNuevo = open('y_train_movil.csv', 'a') 
	outfileNuevo.write(str(datosAnteriores))
	outfileNuevo.close()

	os.chdir(directorioActual)

os.unlink('ProcesarDatos.pyc')


if __name__ == '__main__':
	run(host='192.168.1.33', port=8081, debug=True)
