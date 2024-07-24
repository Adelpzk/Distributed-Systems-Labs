#include <cstdint>
#include <iostream>
#include <fstream>
#include <vector>
#include <sstream>
#include <cassert>

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

void matrix_multiply(const std::vector<mentry_t>& a, const std::vector<mentry_t>& b, std::vector<mentry_t>& c, int m, int rows_per_proc) {
    for (std::size_t ra = 0; ra < rows_per_proc * m; ra = ra + m) { // matrix_a
        for (std::size_t j = 0; j < m; ++j) { // matrix_b
            for (std::size_t ca = ra, rb = j, i = 0; i < m; ++ca, rb = rb + m, ++i) {
                c[ra + j] += a[ca] * b[rb];
            } // for				
        } // for								   
    } // for
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
    std::size_t rows_per_proc = m / process_group_size;

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

    if (process_rank == 0) {
        output_matrix_c.resize(m * n);

        read_matrix(m, n, input_matrix_a, input_filename_a);
        read_matrix(m, n, input_matrix_b, input_filename_b);
       
        assert(input_matrix_a.size() == m * n && "Matrix A size is incorrect");
        assert(input_matrix_b.size() == m * n && "Matrix B size is incorrect");
    }

    std::vector<mentry_t> local_a(rows_per_proc * m);
    std::vector<mentry_t> local_c(rows_per_proc * m, 0);

    // Scatter the rows of matrix A to all processes
    MPI_Scatter(input_matrix_a.data(), rows_per_proc * m, MPI_UINT64_T, local_a.data(), rows_per_proc * m, MPI_UINT64_T, 0, MPI_COMM_WORLD);

    // Broadcast matrix B to all processes
    input_matrix_b.resize(m * n); // Resize for broadcasting
    MPI_Bcast(input_matrix_b.data(), m * n, MPI_UINT64_T, 0, MPI_COMM_WORLD);

    // Perform local matrix multiplication
    matrix_multiply(local_a, input_matrix_b, local_c, m, rows_per_proc);

    // Gather the local matrices C from all processes
    if (process_rank == 0) {
        output_matrix_c.resize(m * n);
    }

    MPI_Gather(local_c.data(), rows_per_proc * m, MPI_UINT64_T, output_matrix_c.data(), rows_per_proc * m, MPI_UINT64_T, 0, MPI_COMM_WORLD);
 
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
