import argparse

# Function to remove all lines starting with "layer" or "layerblock"
def remove_layer_lines(input_file, output_file):
    with open(input_file, 'r') as file:
        lines = file.readlines()

    # Filter out lines starting with "layer" or "layerblock"
    filtered_lines = [line for line in lines if not line.strip().startswith(('layer', 'layerblock'))]

    # Write the modified content back to the file
    with open(output_file, 'w') as file:
        file.writelines(filtered_lines)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Remove all lines starting with 'layer' or 'layerblock' from a FIRRTL file.")
    parser.add_argument("input_file", help="Path to the input FIRRTL file")
    parser.add_argument("output_file", help="Path to save the modified FIRRTL file")

    args = parser.parse_args()

    remove_layer_lines(args.input_file, args.output_file)
