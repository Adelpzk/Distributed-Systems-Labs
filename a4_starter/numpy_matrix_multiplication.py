import sys
import time
import numpy as np
from timeit import default_timer as timer

def read_matrix(r, c, m, filename) :
	with open (filename, "r") as file :
		line_ID = 0
		for line in file :
			line = line.strip(" \t\n\r")
			tokens = line.split(" ")

			if c > len(tokens) :
				print("Error: worng input")	
				return	
			row = [int(i) for i in tokens[:c]]
			m.append(row)

			line_ID = line_ID + 1
			if line_ID == r :
				return
	return

def write_matrix(m, filename) :
	with open (filename, "w") as file :
		for row in range(len(m)) :
			for column in m[row] :
				#print(column, end =" ")
				file.write(str(column) + " ")
			#print("\n")
			file.write("\n")
	return


input_matrix_order = int(sys.argv[1])
input_matrix_g_filename = sys.argv[2]
input_matrix_h_filename = sys.argv[3]
output_matrix_dir = sys.argv[4]

r = input_matrix_order
c = r

g = []
h = []

start_time = timer()

print("Gorund truth read matrices")

read_matrix(r, c, g, input_matrix_g_filename)
read_matrix(r, c, h, input_matrix_h_filename)

matrix_g = np.matrix(g)
matrix_h = np.matrix(h)

print(matrix_g.shape)
print(matrix_h.shape)

end_time = timer()
elapsed_time = end_time - start_time
print("Gorund truth elapsed time: " + str(elapsed_time) + " seconds")

print("Gorund truth matrix multiplication")

start_time = timer()

matrix_gh = matrix_g * matrix_h

end_time = timer()

print(matrix_gh.shape)

elapsed_time = end_time - start_time
print("Gorund truth elapsed time: " + str(elapsed_time) + " seconds")

write_matrix(matrix_gh.tolist(), output_matrix_dir + "/output_" + str((matrix_gh.shape)[0]) + "_x_" + str((matrix_gh.shape)[1]) + ".txt")

sys.exit()
