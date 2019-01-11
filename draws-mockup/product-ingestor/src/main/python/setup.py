from setuptools import setup

dependencies = [
    "draws-mock-icd",
    "drawsmb"
]

setup(name='draws-mock-product-ingestor',
      version='0.1',
      description='DRAWS Mock Product Ingestor',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/mock', 'draws/mock/product-ingestor'],
      install_requires=dependencies,
      zip_safe=False)
