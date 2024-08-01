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

    // Determine block dimensions
    int rows_per_proc = m / rows;
    int cols_per_proc = n / cols;

    // Create Cartesian communicator
    int dims[2] = {rows, cols};
    int periods[2] = {0, 0};
    MPI_Comm cart_comm;
    MPI_Cart_create(MPI_COMM_WORLD, 2, dims, periods, 1, &cart_comm);

    // Get coordinates of the current process in the Cartesian communicator
    int coords[2];
    MPI_Cart_coords(cart_comm, process_rank, 2, coords);
    int row_rank = coords[0];
    int col_rank = coords[1];

    MPI_Comm row_comm, col_comm;
    MPI_Comm_split(cart_comm, row_rank, process_rank, &row_comm);
    MPI_Comm_split(cart_comm, col_rank, process_rank, &col_comm);
    
    // root reads matrices
    if (process_rank == 0) {
        // std::cout << "grid = " << rows << "x" << cols << std::endl;
        // std::cout << "block size = " << rows_per_proc << "x" << cols_per_proc << std::endl;
        output_matrix_c.resize(m * n);
        read_matrix(m, n, input_matrix_a, input_filename_a);
        read_matrix(m, n, input_matrix_b, input_filename_b);
    }

    std::vector<mentry_t> local_a(rows_per_proc * m);
    std::vector<mentry_t> local_b(cols_per_proc * m);
    std::vector<mentry_t> local_c(rows_per_proc * cols_per_proc, 0);

    if (process_rank == 0) {
        int rank_to_send = 0;
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

                if (i == 0 && j == 0) {
                    // Root process itself
                    local_a = rows_of_a;
                    local_b = cols_of_b;
                } else if (i == 0) {
                    // Topmost row processors
                    MPI_Send(cols_of_b.data(), n * cols_per_proc, MPI_UINT64_T, rank_to_send, 1, MPI_COMM_WORLD);
                } else if (j == 0) {
                    // Leftmost column processors
                    MPI_Send(rows_of_a.data(), rows_per_proc * n, MPI_UINT64_T, rank_to_send, 0, MPI_COMM_WORLD);
                }
                rank_to_send++;
            }
        }
    } 

    if (row_rank == 0 && process_rank != 0) {
        // Receive rows of A
        MPI_Recv(local_b.data(), cols_per_proc * n, MPI_UINT64_T, 0, 1, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    }
    if (col_rank == 0 && process_rank != 0) {
        // Receive columns of B
        MPI_Recv(local_a.data(), n * rows_per_proc, MPI_UINT64_T, 0, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
    }

    // Broadcast rows of A within rows
    MPI_Bcast(local_a.data(), rows_per_proc * n, MPI_UINT64_T, 0, row_comm);

    // Broadcast columns of B within columns
    MPI_Bcast(local_b.data(), n * cols_per_proc, MPI_UINT64_T, 0, col_comm);

    // print(local_a, rows_per_proc, n, process_rank, "A");
    // print(local_b, n, cols_per_proc, process_rank, "B");

    // Perform matrix multiplication
    multiply(local_a, rows_per_proc, n, local_b, cols_per_proc, local_c);

    // print(local_c, rows_per_proc, cols_per_proc, process_rank, "C");

    // Gather all blocks from processes
    std::vector<mentry_t> gathered_blocks;
    if (process_rank == 0) {
        gathered_blocks.resize(process_group_size * rows_per_proc * cols_per_proc);
    }
    
    MPI_Gather(local_c.data(), rows_per_proc * cols_per_proc, MPI_UINT64_T, gathered_blocks.data(), rows_per_proc * cols_per_proc, MPI_UINT64_T, 0, MPI_COMM_WORLD);

    // Default gather is incorrect, rearrangement required
    if (process_rank == 0) {
        // Rearrange gathered blocks
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                int rank = i * cols + j;
                int start_row = i * rows_per_proc;
                int start_col = j * cols_per_proc;
                int displ = rank * rows_per_proc * cols_per_proc;
                
                for (int r = 0; r < rows_per_proc; ++r) {
                    for (int c = 0; c < cols_per_proc; ++c) {
                        output_matrix_c[(start_row + r) * n + (start_col + c)] = gathered_blocks[displ + r * cols_per_proc + c];
                    }
                }
            }
        }
        // print(output_matrix_c, m, n, 0, "C");
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
