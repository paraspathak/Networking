/*
    Paras Pathak
    CSE 3320-001 Summer 2019
    Hw 2b Sorting using Threads
    Guide to grader:
        Extract file type make and after completion ./sort
        Choose between 2, 4, and 10 process to create 
        IMPORTANT !!!!!  if you want to change your selection reload ./sort and choose option
        program will then run specified number of threads and total time elapsed since the sort is 
        started and displays the time. The program asks the user if they want to store sorted result.
*/


//C++ Library calls
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <chrono>
#include <algorithm>
#include <thread>


//Writes parts of vector into specified filename
bool write_part_of_vector(const std::vector<double>* data, int start, int end, std::string filename){
    std::ofstream writer{filename};
    if(!writer) return false;
    for(;start!=end; start++){
        writer<<data->at(start)<<"\n";
    }
    return true;
}

void sort_vector(int index, std::vector<double>* earthquake, int increment_number, bool is_4_max_number){
    std::cout<<"Thread "<<index<<" created"<<std::endl;
    int start,end;
    //Allocate which parts of the vector to sort using the index specified
    {
        if(index==1){
        start=0;
        end = increment_number;
        }
        else if(index==2){
            start=increment_number;
            end = start + increment_number;
        }
        else if(index==3){
            start= 2 * increment_number;
            end = start + increment_number;
        }
        else if(index==4){
            start=3*increment_number;
            if(is_4_max_number){
                end = earthquake->size();
            }else{
                end = start + increment_number;
            }
        }
        else if(index==5){
            start=4*increment_number;
            end = start + increment_number;
        }
        else if(index==6){
            start=5*increment_number;
            end = start + increment_number;
        }
        else if(index==7){
            start=6*increment_number;
            end = start + increment_number;
        }
        else if(index==8){
            start=7*increment_number;
            end = start + increment_number;
        }
        else if(index==9){
            start=8*increment_number;
            end = start + increment_number;
        }
        else if(index==10){
            start=9*increment_number;
            end = earthquake->size();
        }
        if(end ==-11 || end>earthquake->size()){
            end = earthquake->size();
        }
    }
    //Uncomment for more info about threads
    //std::cout<<"index  "<<index<<" start  "<<start<<" end  "<<end<<" thread"<<" inc num  "<<increment_number<<std::endl;
    
    //Bubble Sort Algorithm
    for(; start!=end; ++start){
        for(int j=start; j<end; ++j){
            if(earthquake->at(start)>earthquake->at(j)){
                double temp = earthquake->at(start);
                earthquake->at(start)=earthquake->at(j);
                earthquake->at(j)=temp;
            }    
        }
    }
}



int main(){
    auto magnitude = new std::vector<double>;   //Allocate on the heap
    std::vector<std::thread> thread_created;
    int number_thread=0;

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
            std::stringstream convert_double {column.at(4)};
            double temp;
            convert_double>>temp;
            magnitude->push_back(temp); 
        }
    }


    //Splash Screen and ask user for number of process    
    std::cout<<R"(
                This program sorts the earthquake data received from USGS
                using multiple threads

                Please enter how many thread you want to create?

                2                       4                       10

    )";
    std::cin>>number_thread;
    std::cout<<"Creating "<<number_thread<<" Threads....."<<std::endl; 
    auto start = std::chrono::system_clock::now();

    int increment = magnitude->size()/number_thread;

    std::cout<<"Size of input is  "<<magnitude->size()<<"\nOne increment is "<<increment<<std::endl;

    bool change_end_point = false;
    if(number_thread==4){
        change_end_point = true;
    }


    //Create multiple thread for sorting
    for(int i=0; i<number_thread; ++i){
        thread_created.push_back(std::thread{[&]{sort_vector(i,magnitude,increment,change_end_point);}});
    }

    //Join them threads
    for(int i=0; i<number_thread; ++i){
        std::cout<<"Thread "<<i<<" joined"<<std::endl;
        thread_created[i].join();
    }
    //Sort the result now
    std::sort(magnitude->begin(), magnitude->end());
    auto end = std::chrono::system_clock::now();
    std::chrono::duration<double> elapsed_seconds = end-start;
    std::cout<<"Total time from start of sort to end of sort is:"<<elapsed_seconds.count()<<"seconds"<<std::endl;
    std::cout<<"Do you want to save sorted data?\nPress 1 for yes, 0 to exit: ";
    int save_to_file;
    std::cin>>save_to_file;
    if(save_to_file){
        std::cout<<"Please enter filename to save as: ";
        std::string filename;
        std::cin>>filename;
        if(!write_part_of_vector(magnitude,0,magnitude->size(),filename)){
            std::cerr<<"Cannot write out to "<<filename<<std::endl;
        }
    }
}