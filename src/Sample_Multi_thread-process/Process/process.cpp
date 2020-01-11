/*
    Paras Pathak
    CSE 3320-001 Summer 2019
    Hw 2a Sorting using Process
    Guide to grader:
        Extract file type make and after completion ./sort
        Choose between 2, 4, and 10 process to create 
        IMPORTANT !!!!!  if you want to change your selection type make rebuild which will erase temporary files!!!!!!!!
        program will then run specified process and time each process as well as
        total time elapsed since the sort is started and displays the times only
        The program DOESNT store sorted result.
*/


//C++ Library calls
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <chrono>
#include <algorithm>

//C library calls
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <stdlib.h>

//Merges two supplied vectors into one
std::vector<double> merge_vector(std::vector<double>& first, std::vector<double>& second){
    std::vector<double> result;
    result.reserve(first.size()+second.size());
    result.insert(result.end(),first.begin(),first.end());
    result.insert(result.end(),second.begin(),second.end());
    return result;
}

//Writes parts of vector into specified filename
bool write_part_of_vector(std::vector<std::string>& data, int start, int end, std::string filename){
    std::ofstream writer{filename};
    if(!writer) return false;
    for(;start!=end; start++){
        writer<<data.at(start)<<"\n";
    }
    return true;
}

bool write_part_of_vector(std::vector<double>& data, int start, int end, std::string filename){
    std::ofstream writer{filename};
    if(!writer) return false;
    for(;start!=end; start++){
        writer<<data.at(start)<<"\n";
    }
    return true;
}



//Sorts the given vector
void sort_vector(std::vector<double>& storage){
    for(int i=0; i<storage.size(); ++i){
        for(int j=0; j<storage.size(); ++j){
            if(storage.at(i)>storage.at(j)){
                double temp = storage.at(i);
                storage[i]=storage.at(j);
                storage[j]=temp;
            }
        }
    }
}
//Stores items into a vector from a file
bool read_vector(std::string filename, std::vector<double>& storage){
    std::ifstream filereader{filename};
    if(!filereader) return false;
    double temp;
    std::string line;
    while (std::getline(filereader,line)) {
        std::stringstream abc{line};
        abc>>temp;
        storage.push_back(temp);
    }
    return true;
}

//Opens two files and then merges their content into a vector
std::vector<double> merge_files(std::string first, std::string second){
    std::vector<double> first_storage;
    std::vector<double> second_storage;
    read_vector(first,first_storage);
    read_vector(second, second_storage);
    
    return merge_vector(first_storage,second_storage);
}

//Writes contents of a vector into a file
bool write_vector(std::string filename, std::vector<double>& storage){
    std::ofstream filewriter{filename};
    if(!filewriter) return false;
    for(auto item : storage){
        filewriter<<item<<"\n";
    }
    return true;
}


int main(){
    /*
        Note to grader: Scope is forced upon for each task!
    */

    //Variables used throughout main
    std::vector<std::string> magnitude;
    int number_process=0;

    //Read data from the CSV file and store magnitude into a vector
    {
        std::ifstream ifs {"all_month_data.csv"};
        std::string line;
        std::getline(ifs, line);    //First column only has string denoting content of the column
        int i =0;
        while (std::getline(ifs,line)) {
            std::stringstream line_to_parse{line};
            std::string cell;
            std::vector<std::string> column;
            while (std::getline(line_to_parse,cell,',')) {
                column.push_back(cell);
            }
            magnitude.push_back(column.at(4)); 
        }
    }

    //Splash Screen and ask user for number of process
    
    std::cout<<R"(
                This program sorts the earthquake data received from USGS
                using multiple processess

                Please enter how many process you want to create?

                2                       4                       10

    )";
    std::cin>>number_process;
    std::cout<<"Creating "<<number_process<<" Process....."<<std::endl; 
    auto start = std::chrono::system_clock::now();
        
    
    //Create different files for the process to sort
    {
        std::vector<std::string> names;
        for(int i=0; i<5; i++){     //To create names of temporary files to communicate between process
            names.push_back((std::to_string(i)+"out"));
            names.push_back(("out"+std::to_string(i)));
        }
        int start_point =0, end_point = magnitude.size()/number_process, upgrade = magnitude.size()/number_process ;
        for(int i=0; i<number_process; ++i){ 
            if(i==(number_process-1)) end_point=magnitude.size();   //Handling the case when the magnitude's size is odd
            write_part_of_vector(magnitude,start_point,end_point,names.at(i));
            start_point=end_point;
            end_point+=upgrade;        
        }
    }

    //Create different process as asked by the menu
    {
        int number_process_half=number_process/2;
                    
        std::vector<int> process_id;
        //Forking different process and then sorting them
        for(int kid=0; kid<number_process_half; kid++){
            process_id.push_back(fork());
            if(process_id.back()<0){
                exit(EXIT_FAILURE); //Could not create a new process
            }
            else if(process_id.back()>0){
                /*Parent process*/
                auto start_time =  std::chrono::system_clock::now();
                std::vector <double> parent_container;  //Only reference to this vector is passed around
                if(read_vector((std::to_string(kid)+"out"),parent_container)){
                    sort_vector(parent_container);
                    if(!write_vector((std::to_string(kid)+"out.cat"),parent_container)){
                        std::cerr<<"Cannot write to files"<<std::endl;
                        exit(1);
                    }
                }                
                auto end_time =  std::chrono::system_clock::now();
                std::chrono::duration<double> elapsed_time = end_time-start_time;
                std::cout<<"One of the generated process took:"<<elapsed_time.count()<<"seconds"<<std::endl;
            }
            else{
                /* Child Process */
                auto start_time =  std::chrono::system_clock::now();
                std::vector<double> child_container;
                if(read_vector(("out"+std::to_string(kid)),child_container)){
                    sort_vector(child_container);
                    if(!write_vector(("out"+std::to_string(kid)+".cat"),child_container)){
                        std::cerr<<"Cannot write to files"<<std::endl;
                        exit(1);
                    }
                }                
                auto end_time =  std::chrono::system_clock::now();
                std::chrono::duration<double> elapsed_time = end_time-start_time;
                std::cout<<"One of the generated process took:"<<elapsed_time.count()<<"seconds"<<std::endl;
                exit(0);
            }    
        }

        for (auto item : process_id){
            int status;
            pid_t pid = wait(&status);
        }
        wait(NULL);    
    }


    //Merge back all the files into one
    {
        if(number_process==2){
            auto a = merge_files("0out","out0");
            std::sort(a.begin(), a.end());
            auto end = std::chrono::system_clock::now();
            std::chrono::duration<double> elapsed_seconds = end-start;
            std::cout<<"Total Time is:  "<<elapsed_seconds.count()<<std::endl;
            std::cout<<"Writing vector out to output.txt"<<std::endl;
            write_part_of_vector(a,0,a.size(), "output.txt");            
        }
        else if(number_process==4){
            auto a = merge_files("0out","out0");
            auto b = merge_files("1out","out1");
            auto c = merge_vector(a,b);
            std::sort(c.begin(),c.end());
            auto end = std::chrono::system_clock::now();
            std::chrono::duration<double> elapsed_seconds = end-start;
            std::cout<<"Total Time is:  "<<elapsed_seconds.count()<<std::endl;
            std::cout<<"Writing vector out to output.txt"<<std::endl;
            write_part_of_vector(c,0,c.size(), "output.txt");
        }
        else if (number_process==10){
            auto a = merge_files("0out","out0");
            auto b = merge_files("1out","out1");
            auto c = merge_files("2out","out2"); 
            auto d = merge_files("3out","out3");
            auto e = merge_files("4out","out4");

            auto f = merge_vector(a,b);
            auto g = merge_vector(c,d);
            auto h = merge_vector(e,f);

            auto result = merge_vector(g,h);
            std::sort(result.begin(), result.end());
            auto end = std::chrono::system_clock::now();
            std::chrono::duration<double> elapsed_seconds = end-start;
            std::cout<<"Total Time is:  "<<elapsed_seconds.count()<<std::endl;
            std::cout<<"Writing vector out to output.txt"<<std::endl;
            write_part_of_vector(result,0,result.size(), "output.txt");
        }     
    }
    
}