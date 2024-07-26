#include <cstdint>
#include <iostream>
#include <fstream>
#include <vector>
#include <sstream>
#include <cassert>
#include <cmath>

#include <stdio.h>
#include <mpi.h>

using mentry_t = std::uint64_t;

// do not modify
void read_matrix(const std::size_t m, const std::size_t n, std::vector<mentry_t>& matrix, const std::string filename) {

    std::ifstream file(filename, std::ifstream::in);  
    if (file.fail()) {
        std::cerr << "File error." << std::endl;
        return;
    }        
    
    std::string line;
    std::size_t line_count = 0; 
    while (std::getline(file, line) && line_count < m) {
        //std::cout << line << std::endl;
        std::istringstream ss(line);
        mentry_t e;    
        for (std::size_t i = 0; i < n; ++i) {
        ss >> e;
        matrix.emplace_back(e);
        }    
        line_count++;    
    }      
    file.close();
} // read_matrix

// do not modify
void write_matrix(const std::size_t m, const std::size_t n, const std::vector<mentry_t>& matrix, const std::string filename) {

    std::ofstream file(filename, std::ofstream::out);
    if (file.fail()) {
        std::cerr << "File error." << std::endl;
        return;
    }

    std::size_t c = 0;
    for (auto e : matrix) {
        if (c == n - 1) {
        file << e << "\n";
        //std::cout << e << std::endl;
        c = 0;
        } else {          
        file << e << " ";
        //std::cout << e << " ";
        c++;
        }
    }  
    file.close();
} // write_matrix
  
// do not modify
void write_result(const std::vector<std::string>& result, const std::string filename) {

    std::ofstream file(filename, std::ofstream::app); //std::ofstream::out);
    if (file.fail()) {
        std::cerr << "File error." << std::endl;
        return;
    }

    for (auto e : result) {
        file << e << ", ";
        std::cout << e << ", ";		  
    }
    file << "\n";   
    std::cout << std::endl;
    file.close(); 		
} // write_result

// Function to print a matrix in row-major order
void print(const std::vector<mentry_t>& matrix, int rows, int cols, int rank, std::string name) {
    if (matrix.size() != rows * cols) {
        std::cerr << "Error: Matrix size does not match the specified dimensions." << std::endl;
        return;
    }

    std::cout << "Rank " << rank << " " << name << std::endl;
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            std::cout << matrix[i * cols + j] << " ";
        }
        std::cout << std::endl;
    }
}

// Function to multiply two matrices with different storage orders
// a: m x n, b: n x p, c: m * n
void multiply(
    const std::vector<mentry_t>& a, int m, int n,
    const std::vector<mentry_t>& b, int p,
    std::vector<mentry_t>& c) { 

    // Perform matrix multiplication
    for (int i = 0; i < m; ++i) {           // Iterate over rows of A (and C)
        for (int j = 0; j < p; ++j) {       // Iterate over columns of B (and C)
            for (int k = 0; k < n; ++k) {   // Iterate over common dimension
                c[i * p + j] += a[i * n + k] * b[k * p + j];
            }
        }
    }
}

int main(int argc, char** argv) {

    int process_rank, process_group_size;

    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &process_group_size);
    MPI_Comm_rank(MPI_COMM_WORLD, &process_rank);

    double start_time;
    double end_time;
    double elapsed_time;

    std::size_t m = std::stoul(argv[1]); //4; // #rows
    std::size_t n = m; // #columns

    std::string input_filename_a = argv[2]; //"matrix_a.txt";
    std::string input_filename_b = argv[3]; //"matrix_b.txt";
    std::string output_filename_c = argv[4]; //"matrix_c.txt";
    std::string output_filename_result = argv[5]; //"a4_result.txt";

    std::string input_experiment_name = argv[6]; // "d";

    // MPI collective operations require elements must be continuous in memory
    std::vector<mentry_t> input_matrix_a;
    std::vector<mentry_t> input_matrix_b;			 
    std::vector<mentry_t> output_matrix_c;

    std::vector<std::string> result;
    
    result.emplace_back(input_experiment_name);

    {
        std::stringstream ss;
        ss << process_group_size;
        result.emplace_back(ss.str());
    }

    {
        std::stringstream ss;		   
        ss << m;
        result.emplace_back(ss.str());
    }

    start_time = MPI_Wtime();

    // do not modify the code above  

    // your code begins //////////////////////////////////////////////////////////

    // You can implement your own "read_matrix" method (e.g., overlap file reading 
    // with matrix partitioning)
    // Only the rank 0 is allowed to read input matrices from files and write 
    // output to file
    // The output matrix must be stored in the "output_matrix_c" data structure
    // The code for writing output to file is provided below 

    // Determine the processor grid dimensions
    int rows = std::sqrt(process_group_size);
    while (process_group_size % rows != 0) {
        rows--;
    }
    int cols = process_group_size / rows;

    std::size_t rows_per_proc = m / rows;
    std::size_t cols_per_proc = n / cols;
    
    if (process_rank == 0) {
        // std::cout << "grid = " << rows << "x" << cols << std::endl;
        // std::cout << "block size = " << rows_per_proc << "x" << cols_per_proc << std::endl;
        
        output_matrix_c.resize(m * n);

        read_matrix(m, n, input_matrix_a, input_filename_a);
        read_matrix(m, n, input_matrix_b, input_filename_b);
       
        assert(input_matrix_a.size() == m * n && "Matrix A size is incorrect");
        assert(input_matrix_b.size() == m * n && "Matrix B size is incorrect");
    }

    std::vector<mentry_t> local_a(rows_per_proc * m);
    std::vector<mentry_t> local_b(cols_per_proc * m);
    std::vector<mentry_t> local_c(rows_per_proc * cols_per_proc, 0);

    if (process_rank == 0) {
        // Scatter rows of matrix A and columns of matrix B
        int rank_to_send = 1;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                int start_row = i * rows_per_proc;
                int start_col = j * cols_per_proc;

                std::vector<mentry_t> rows_of_a(rows_per_proc * n);
                std::vector<mentry_t> cols_of_b(n * cols_per_proc);

                // Extract rows of A
                for (int r = 0; r < rows_per_proc; ++r) {
                    for (int c = 0; c < n; ++c) {
                        rows_of_a[r * n + c] = input_matrix_a[(start_row + r) * n + c];
                    }
                }

                // Extract columns of B
                for (int c = 0; c < cols_per_proc; ++c) {
                    for (int r = 0; r < n; ++r) {
                        cols_of_b[r * cols_per_proc + c] = input_matrix_b[r * n + (start_col + c)];
                    }
                }

                // print(rows_of_a, rows_per_proc, n, 0, "Rows of A");
                // print(cols_of_b, cols_per_proc, n, 0, "Cols of B");

                if (i == 0 && j == 0) {
                    // This is the root process itself
                    // Do not send to itself, only send to others
                    local_a = rows_of_a;
                    local_b = cols_of_b;
                } else {
                    MPI_Send(rows_of_a.data(), rows_per_proc * n, MPI_UINT64_T, rank_to_send, 0, MPI_COMM_WORLD);
                    MPI_Send(cols_of_b.data(), n * cols_per_proc, MPI_UINT64_T, rank_to_send, 1, MPI_COMM_WORLD);
                    rank_to_send++;
                }
            }
        }
    } else {
        MPI_Recv(local_a.data(), rows_per_proc * n, MPI_UINT64_T, 0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
        MPI_Recv(local_b.data(), n * cols_per_proc, MPI_UINT64_T, 0, 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    }

    // print(local_a, rows_per_proc, n, process_rank, "A");
    // print(local_b, n, cols_per_proc, process_rank, "B");

    multiply(local_a, rows_per_proc, n, local_b, cols_per_proc, local_c);

    // print(local_c, rows_per_proc, cols_per_proc, process_rank, "C");
    
    if (process_rank == 0) {
        output_matrix_c.resize(m * n);

        // Displacements and counts
        std::vector<int> displacements(process_group_size);
        std::vector<int> recv_counts(process_group_size, rows_per_proc * cols_per_proc);

        int displ = 0;
        for (int i = 0; i < process_group_size; ++i) {
            displacements[i] = displ;
            displ += rows_per_proc * cols_per_proc;
        }

        // Receive blocks from all processes
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                int rank = i * cols + j;
                if (rank == 0) {
                    std::copy(local_c.begin(), local_c.end(), output_matrix_c.begin() + displacements[rank]);
                } else {
                    MPI_Recv(&output_matrix_c[displacements[rank]], rows_per_proc * cols_per_proc, MPI_UINT64_T, rank, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                }
            }
        }

        // Reassemble matrix C correctly
        std::vector<uint64_t> temp_matrix(m * n, 0);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                int rank = i * cols + j;
                int start_row = i * rows_per_proc;
                int start_col = j * cols_per_proc;
                for (int r = 0; r < rows_per_proc; ++r) {
                    for (int c = 0; c < cols_per_proc; ++c) {
                        temp_matrix[(start_row + r) * n + (start_col + c)] = output_matrix_c[displacements[rank] + r * cols_per_proc + c];
                    }
                }
            }
        }
        output_matrix_c = temp_matrix;

        // print(output_matrix_c, m, n, 0, "C");

    } else {
        // Send local block to the root process
        MPI_Send(local_c.data(), rows_per_proc * cols_per_proc, MPI_UINT64_T, 0, 0, MPI_COMM_WORLD);
    }
 
    // your code ends //////////////////////////////////////////////////////////// 

    // do not modify the code below

    MPI_Barrier(MPI_COMM_WORLD);

    end_time = MPI_Wtime(); // must be after the barrier
    elapsed_time = end_time - start_time;

    if (process_rank == 0) {
        std::cout << "Matrix multiplication computation time: " << elapsed_time << 
        " seconds " << std::endl;
        {
        std::stringstream ss;
        ss << elapsed_time;
        result.emplace_back(ss.str());
        } 	
    }	

    MPI_Barrier(MPI_COMM_WORLD); 

    if (process_rank == 0) {
        double local_start_time =  MPI_Wtime(); 
        write_matrix(m, n, output_matrix_c, output_filename_c);  
        double local_end_time =  MPI_Wtime();  
        double local_elapsed_time = local_end_time - local_start_time;
        std::cout << "MPI rank " << process_rank << " - write output time: " << 
        local_elapsed_time << " seconds " << std::endl; 	
    } 		   

    MPI_Barrier(MPI_COMM_WORLD);

    end_time = MPI_Wtime(); // must be after the barrier
    elapsed_time = end_time - start_time;

    if (process_rank == 0) {
        std::cout << "Matrix multiplication total time: " << elapsed_time << 
        " seconds " << std::endl;
        {
        std::stringstream ss;
        ss << elapsed_time;
        result.emplace_back(ss.str());
        }
        write_result(result, output_filename_result);	
    }  

    MPI_Finalize();
    return 0;
}
